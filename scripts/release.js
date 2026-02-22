#!/usr/bin/env node

/**
 * Release script for capacitor-pica-network-logger
 *
 * Usage:
 *   npm run release                # prompts for version bump type
 *   npm run release -- patch       # patch bump (0.2.3 → 0.2.4)
 *   npm run release -- minor       # minor bump (0.2.3 → 0.3.0)
 *   npm run release -- major       # major bump (0.2.3 → 1.0.0)
 *   npm run release -- 1.0.0       # explicit version
 */

const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');
const { select, confirm } = require('@inquirer/prompts');

const ROOT = path.resolve(__dirname, '..');
const PKG_PATH = path.join(ROOT, 'package.json');
const CHANGELOG_PATH = path.join(ROOT, 'CHANGELOG.md');

function run(cmd, opts = {}) {
    return execSync(cmd, { cwd: ROOT, encoding: 'utf8', stdio: opts.silent ? 'pipe' : 'inherit', ...opts });
}

function runSilent(cmd) {
    try {
        return run(cmd, { silent: true }).trim();
    } catch {
        return '';
    }
}

function readPkg() {
    return JSON.parse(fs.readFileSync(PKG_PATH, 'utf8'));
}

function bumpVersion(current, type) {
    const [major, minor, patch] = current.split('.').map(Number);
    switch (type) {
        case 'patch': return `${major}.${minor}.${patch + 1}`;
        case 'minor': return `${major}.${minor + 1}.0`;
        case 'major': return `${major + 1}.0.0`;
        default: return type; // explicit version string
    }
}

function today() {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function updateChangelog(version) {
    let changelog = fs.readFileSync(CHANGELOG_PATH, 'utf8');

    const unreleasedRegex = /## \[Unreleased\]\n([\s\S]*?)(?=\n## \[|$)/;
    const match = changelog.match(unreleasedRegex);

    if (!match || !match[1].trim()) {
        console.error('No [Unreleased] section with content found in CHANGELOG.md');
        console.error('Add your changes under ## [Unreleased] before releasing.');
        process.exit(1);
    }

    const notes = match[1].trim();
    const newSection = `## [Unreleased]\n\n## [${version}] - ${today()}\n\n${notes}`;
    changelog = changelog.replace(unreleasedRegex, newSection + '\n');

    fs.writeFileSync(CHANGELOG_PATH, changelog, 'utf8');
    return notes;
}

function hasGh() {
    try {
        execSync('gh --version', { stdio: 'pipe' });
        return true;
    } catch {
        return false;
    }
}

function isNpmLoggedIn() {
    try {
        execSync('npm whoami', { stdio: 'pipe' });
        return true;
    } catch {
        return false;
    }
}

async function ensureNpmAuth() {
    if (isNpmLoggedIn()) {
        const user = runSilent('npm whoami');
        console.log(`Logged in to npm as: ${user}`);
        return;
    }

    console.log('Not logged in to npm.');
    const shouldLogin = await confirm({ message: 'Run npm login now?' });
    if (!shouldLogin) {
        console.error('Cannot publish without npm authentication. Aborting.');
        process.exit(1);
    }

    run('npm login', { stdio: 'inherit' });

    if (!isNpmLoggedIn()) {
        console.error('npm login failed. Aborting.');
        process.exit(1);
    }

    const user = runSilent('npm whoami');
    console.log(`Logged in to npm as: ${user}`);
}

async function main() {
    // Ensure working tree is clean
    const status = runSilent('git status --porcelain');
    if (status) {
        console.log('Working tree has uncommitted changes:\n' + status);
        const proceed = await confirm({ message: 'Continue anyway?', default: false });
        if (!proceed) process.exit(1);
    }

    const pkg = readPkg();
    const currentVersion = pkg.version;
    console.log(`Current version: ${currentVersion}`);

    // Determine new version
    let input = process.argv[2];
    if (!input) {
        const [major, minor, patch] = currentVersion.split('.').map(Number);
        input = await select({
            message: 'Version bump type:',
            choices: [
                { name: `patch (${major}.${minor}.${patch + 1})`, value: 'patch' },
                { name: `minor (${major}.${minor + 1}.0)`, value: 'minor' },
                { name: `major (${major + 1}.0.0)`, value: 'major' },
            ],
        });
    }

    const newVersion = bumpVersion(currentVersion, input);
    if (!/^\d+\.\d+\.\d+$/.test(newVersion)) {
        console.error(`Invalid version: ${newVersion}`);
        process.exit(1);
    }

    const proceed = await confirm({
        message: `Release ${currentVersion} → ${newVersion}?`,
    });
    if (!proceed) process.exit(0);

    // 1. Update CHANGELOG.md
    console.log('\nUpdating CHANGELOG.md...');
    const releaseNotes = updateChangelog(newVersion);

    // 2. Bump version in package.json (no git tag from npm)
    console.log('Bumping package.json version...');
    run(`npm version ${newVersion} --no-git-tag-version`);

    // 3. Build
    console.log('Building...');
    run('npm run build');

    // 4. Commit and tag
    console.log('Committing and tagging...');
    run('git add -A');
    run(`git commit -m "chore(release): v${newVersion}"`);
    run(`git tag v${newVersion}`);

    // 5. Push
    console.log('Pushing to remote...');
    run('git push && git push --tags');

    // 6. Ensure npm auth and publish
    await ensureNpmAuth();
    console.log('Publishing to npm...');
    run('npm publish');

    // 7. Create GitHub Release if gh is available
    if (hasGh()) {
        console.log('Creating GitHub Release...');
        const notesFile = path.join(ROOT, 'tmp', '.release-notes');
        fs.mkdirSync(path.dirname(notesFile), { recursive: true });
        fs.writeFileSync(notesFile, releaseNotes, 'utf8');
        run(`gh release create v${newVersion} --title "v${newVersion}" --notes-file "${notesFile}"`);
        fs.unlinkSync(notesFile);
    } else {
        console.log('gh CLI not found, skipping GitHub Release creation.');
        console.log('Install gh (https://cli.github.com) to auto-create releases.');
    }

    console.log(`\nReleased v${newVersion}`);
}

main().catch((err) => {
    console.error(err);
    process.exit(1);
});
