#if canImport(UIKit)
import UIKit
#endif

#if canImport(UIKit)
final class InspectorViewController: UIViewController, UITableViewDataSource, UITableViewDelegate, UISearchResultsUpdating {
    private let tableView = UITableView(frame: .zero, style: .plain)
    private var logs: [LogEntry] = []
    private var filtered: [LogEntry] = []
    private let searchController = UISearchController(searchResultsController: nil)
    private var isDark: Bool = false

    override func viewDidLoad() {
        super.viewDidLoad()
        isDark = traitCollection.userInterfaceStyle == .dark
        title = "Network Inspector"
        view.backgroundColor = InspectorTheme.background(isDark)

        navigationItem.leftBarButtonItem = UIBarButtonItem(barButtonSystemItem: .close, target: self, action: #selector(close))
        navigationItem.rightBarButtonItems = [
            UIBarButtonItem(title: "Clear", style: .plain, target: self, action: #selector(clearAll)),
            UIBarButtonItem(image: UIImage(systemName: isDark ? "sun.max" : "moon"), style: .plain, target: self, action: #selector(toggleTheme))
        ]

        searchController.obscuresBackgroundDuringPresentation = false
        searchController.searchResultsUpdater = self
        searchController.searchBar.placeholder = "Filter"
        navigationItem.searchController = searchController
        navigationItem.hidesSearchBarWhenScrolling = false

        tableView.translatesAutoresizingMaskIntoConstraints = false
        tableView.register(InspectorLogCell.self, forCellReuseIdentifier: InspectorLogCell.reuseId)
        tableView.dataSource = self
        tableView.delegate = self
        tableView.rowHeight = UITableView.automaticDimension
        tableView.estimatedRowHeight = 84
        tableView.separatorStyle = .none
        tableView.backgroundColor = InspectorTheme.background(isDark)
        view.addSubview(tableView)

        NSLayoutConstraint.activate([
            tableView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            tableView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            tableView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            tableView.bottomAnchor.constraint(equalTo: view.bottomAnchor)
        ])
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        reloadData()
    }

    private func reloadData() {
        logs = LogRepository.shared.getLogEntries()
        applyFilter(searchController.searchBar.text)
    }

    private func applyFilter(_ query: String?) {
        let text = (query ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        if text.count < 3 {
            filtered = logs
        } else {
            filtered = logs.filter { entry in
                entry.url.localizedCaseInsensitiveContains(text) ||
                    entry.host?.localizedCaseInsensitiveContains(text) == true ||
                    entry.method.localizedCaseInsensitiveContains(text)
            }
        }
        tableView.reloadData()
    }

    @objc private func close() {
        dismiss(animated: true)
    }

    @objc private func clearAll() {
        LogRepository.shared.clear()
        reloadData()
    }

    @objc private func toggleTheme() {
        isDark.toggle()
        overrideUserInterfaceStyle = isDark ? .dark : .light
        view.backgroundColor = InspectorTheme.background(isDark)
        tableView.backgroundColor = InspectorTheme.background(isDark)
        navigationItem.rightBarButtonItems?.last?.image = UIImage(systemName: isDark ? "sun.max" : "moon")
        tableView.reloadData()
    }

    func updateSearchResults(for searchController: UISearchController) {
        applyFilter(searchController.searchBar.text)
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return filtered.count
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        guard let cell = tableView.dequeueReusableCell(withIdentifier: InspectorLogCell.reuseId, for: indexPath) as? InspectorLogCell else {
            return UITableViewCell()
        }
        cell.configure(with: filtered[indexPath.row], isDark: isDark)
        return cell
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        let detail = InspectorDetailViewController(log: filtered[indexPath.row], isDark: isDark)
        navigationController?.pushViewController(detail, animated: true)
    }
}

final class InspectorLogCell: UITableViewCell {
    static let reuseId = "InspectorLogCell"
    private let container = UIView()
    private let titleLabel = UILabel()
    private let hostLabel = UILabel()
    private let dateLabel = UILabel()
    private let detailLabel = UILabel()
    private let statusChip = InspectorChipLabel()
    private let methodChip = InspectorChipLabel()
    private let headerRow = UIStackView()
    private let bottomRow = UIStackView()

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        selectionStyle = .none
        backgroundColor = .clear
        contentView.backgroundColor = .clear

        container.translatesAutoresizingMaskIntoConstraints = false
        container.layer.cornerRadius = 14
        container.layer.masksToBounds = true
        contentView.addSubview(container)

        // Row 1: [statusChip] [methodChip] --- dateLabel
        headerRow.axis = .horizontal
        headerRow.spacing = 8
        headerRow.alignment = .center
        headerRow.translatesAutoresizingMaskIntoConstraints = false

        dateLabel.font = UIFont.systemFont(ofSize: 11, weight: .medium)
        dateLabel.numberOfLines = 1
        dateLabel.textAlignment = .right
        dateLabel.setContentHuggingPriority(.defaultLow, for: .horizontal)
        dateLabel.setContentCompressionResistancePriority(.required, for: .horizontal)

        let headerSpacer = UIView()
        headerSpacer.setContentHuggingPriority(.defaultLow, for: .horizontal)

        headerRow.addArrangedSubview(statusChip)
        headerRow.addArrangedSubview(methodChip)
        headerRow.addArrangedSubview(headerSpacer)
        headerRow.addArrangedSubview(dateLabel)

        // Row 2: path (full width, unlimited lines)
        titleLabel.font = UIFont.systemFont(ofSize: 15, weight: .semibold)
        titleLabel.numberOfLines = 0

        // Row 3: hostLabel --- detailLabel (duration • size)
        bottomRow.axis = .horizontal
        bottomRow.spacing = 8
        bottomRow.alignment = .center
        bottomRow.translatesAutoresizingMaskIntoConstraints = false

        hostLabel.font = UIFont.systemFont(ofSize: 13, weight: .regular)
        hostLabel.numberOfLines = 1
        hostLabel.lineBreakMode = .byTruncatingTail
        hostLabel.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
        hostLabel.setContentHuggingPriority(.defaultLow, for: .horizontal)

        detailLabel.font = UIFont.systemFont(ofSize: 11, weight: .medium)
        detailLabel.numberOfLines = 1
        detailLabel.textAlignment = .right
        detailLabel.setContentCompressionResistancePriority(.required, for: .horizontal)
        detailLabel.setContentHuggingPriority(.required, for: .horizontal)

        bottomRow.addArrangedSubview(hostLabel)
        bottomRow.addArrangedSubview(detailLabel)

        let stack = UIStackView(arrangedSubviews: [headerRow, titleLabel, bottomRow])
        stack.axis = .vertical
        stack.spacing = 6
        stack.translatesAutoresizingMaskIntoConstraints = false
        container.addSubview(stack)

        NSLayoutConstraint.activate([
            container.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 8),
            container.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 12),
            container.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -12),
            container.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -8),
            stack.topAnchor.constraint(equalTo: container.topAnchor, constant: 12),
            stack.leadingAnchor.constraint(equalTo: container.leadingAnchor, constant: 12),
            stack.trailingAnchor.constraint(equalTo: container.trailingAnchor, constant: -12),
            stack.bottomAnchor.constraint(equalTo: container.bottomAnchor, constant: -12)
        ])
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    func configure(with log: LogEntry, isDark: Bool) {
        container.backgroundColor = InspectorTheme.cardBackground(isDark)
        titleLabel.textColor = InspectorTheme.textPrimary(isDark)
        hostLabel.textColor = InspectorTheme.textSecondary(isDark)
        dateLabel.textColor = InspectorTheme.textSecondary(isDark)
        detailLabel.textColor = InspectorTheme.textSecondary(isDark)

        let statusText = log.resStatus.map { "\($0)" } ?? "-"
        statusChip.configure(text: statusText, background: InspectorTheme.statusColor(log.resStatus), content: InspectorTheme.contentColor(for: InspectorTheme.statusColor(log.resStatus)))
        methodChip.configure(text: log.method.isEmpty ? "-" : log.method, background: InspectorTheme.methodColor(log.method), content: InspectorTheme.contentColor(for: InspectorTheme.methodColor(log.method)))

        let path = log.path ?? URL(string: log.url)?.path ?? ""
        titleLabel.text = path.isEmpty ? log.url : path
        hostLabel.text = log.host ?? URL(string: log.url)?.host ?? ""
        dateLabel.text = InspectorTheme.formatTime(log.startTs)
        let duration = "\(log.durationMs ?? 0) ms"
        let size = InspectorTheme.formatSize(log.resBody?.count ?? 0)
        detailLabel.text = [duration, size].joined(separator: "  •  ")
    }
}

final class InspectorChipLabel: UILabel {
    private let insets = UIEdgeInsets(top: 4, left: 8, bottom: 4, right: 8)

    override init(frame: CGRect) {
        super.init(frame: frame)
        font = UIFont.systemFont(ofSize: 11, weight: .semibold)
        layer.cornerRadius = 8
        layer.masksToBounds = true
        setContentHuggingPriority(.required, for: .horizontal)
        setContentCompressionResistancePriority(.required, for: .horizontal)
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    func configure(text: String, background: UIColor, content: UIColor) {
        self.text = text
        self.backgroundColor = background
        self.textColor = content
    }

    override func drawText(in rect: CGRect) {
        super.drawText(in: rect.inset(by: insets))
    }

    override var intrinsicContentSize: CGSize {
        let size = super.intrinsicContentSize
        return CGSize(width: size.width + insets.left + insets.right, height: size.height + insets.top + insets.bottom)
    }
}

final class InspectorDetailViewController: UIViewController {
    private let log: LogEntry
    private var isDark: Bool
    private let segmented = UISegmentedControl(items: ["Request", "Response"])
    private let scrollView = UIScrollView()
    private let stack = UIStackView()

    init(log: LogEntry, isDark: Bool) {
        self.log = log
        self.isDark = isDark
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        title = "Transaction"
        view.backgroundColor = InspectorTheme.background(isDark)
        navigationItem.rightBarButtonItem = UIBarButtonItem(barButtonSystemItem: .action, target: self, action: #selector(share))

        segmented.selectedSegmentIndex = 0
        segmented.addTarget(self, action: #selector(segmentChanged), for: .valueChanged)
        segmented.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(segmented)

        scrollView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(scrollView)

        stack.axis = .vertical
        stack.spacing = 12
        stack.translatesAutoresizingMaskIntoConstraints = false
        scrollView.addSubview(stack)

        NSLayoutConstraint.activate([
            segmented.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 12),
            segmented.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16),
            segmented.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -16),
            scrollView.topAnchor.constraint(equalTo: segmented.bottomAnchor, constant: 12),
            scrollView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            scrollView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            scrollView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            stack.topAnchor.constraint(equalTo: scrollView.topAnchor, constant: 16),
            stack.leadingAnchor.constraint(equalTo: scrollView.leadingAnchor, constant: 16),
            stack.trailingAnchor.constraint(equalTo: scrollView.trailingAnchor, constant: -16),
            stack.bottomAnchor.constraint(equalTo: scrollView.bottomAnchor, constant: -16),
            stack.widthAnchor.constraint(equalTo: scrollView.widthAnchor, constant: -32)
        ])

        refreshContent()
    }

    @objc private func segmentChanged() {
        refreshContent()
    }

    @objc private func share() {
        let sheet = UIAlertController(title: "Share", message: nil, preferredStyle: .actionSheet)
        sheet.addAction(UIAlertAction(title: "Share cURL", style: .default) { [weak self] _ in
            self?.shareText(self?.buildCurl() ?? "")
        })
        sheet.addAction(UIAlertAction(title: "Share txt", style: .default) { [weak self] _ in
            self?.shareText(self?.buildShareText() ?? "")
        })
        sheet.addAction(UIAlertAction(title: "Share HAR", style: .default) { [weak self] _ in
            self?.shareText(self?.buildHar() ?? "")
        })
        sheet.addAction(UIAlertAction(title: "Cancel", style: .cancel))
        if let popover = sheet.popoverPresentationController {
            popover.barButtonItem = navigationItem.rightBarButtonItem
        }
        present(sheet, animated: true)
    }

    private func refreshContent() {
        stack.arrangedSubviews.forEach { $0.removeFromSuperview() }
        stack.addArrangedSubview(overviewSection())
        if segmented.selectedSegmentIndex == 0 {
            stack.addArrangedSubview(sectionCard(title: "Request Headers", body: formatHeaders(log.reqHeadersJson), isHeader: true))
            stack.addArrangedSubview(sectionCard(title: "Request Body", body: formatBody(log.reqBody), isHeader: false))
        } else {
            stack.addArrangedSubview(sectionCard(title: "Response Headers", body: formatHeaders(log.resHeadersJson), isHeader: true))
            stack.addArrangedSubview(sectionCard(title: "Response Body", body: formatBody(log.resBody), isHeader: false))
        }
    }

    private func overviewSection() -> UIView {
        let container = UIView()
        let stack = UIStackView()
        stack.axis = .vertical
        stack.spacing = 8
        stack.translatesAutoresizingMaskIntoConstraints = false
        container.addSubview(stack)

        let rows: [(String, String)] = [
            ("URL", log.url),
            ("Status", log.resStatus.map { "\($0)" } ?? "-"),
            ("Duration", "\(log.durationMs ?? 0) ms"),
            ("Size", InspectorTheme.formatSize(log.resBody?.count ?? 0)),
            ("Started", InspectorTheme.formatTime(log.startTs)),
            ("Protocol", log.protocol ?? "-"),
            ("SSL", log.ssl ? "Yes" : "No")
        ]
        rows.forEach { key, value in
            stack.addArrangedSubview(keyValueRow(key: key, value: value))
        }

        NSLayoutConstraint.activate([
            stack.topAnchor.constraint(equalTo: container.topAnchor),
            stack.leadingAnchor.constraint(equalTo: container.leadingAnchor),
            stack.trailingAnchor.constraint(equalTo: container.trailingAnchor),
            stack.bottomAnchor.constraint(equalTo: container.bottomAnchor)
        ])
        return container
    }

    private func keyValueRow(key: String, value: String) -> UIView {
        let row = UIStackView()
        row.axis = .horizontal
        row.spacing = 12
        row.alignment = .top
        let keyLabel = UILabel()
        keyLabel.font = UIFont.systemFont(ofSize: 11, weight: .semibold)
        keyLabel.textColor = InspectorTheme.textPrimary(isDark)
        keyLabel.text = key
        keyLabel.widthAnchor.constraint(equalToConstant: 86).isActive = true
        let valueLabel = UILabel()
        valueLabel.font = UIFont.systemFont(ofSize: 13, weight: .regular)
        valueLabel.textColor = InspectorTheme.textPrimary(isDark)
        valueLabel.text = value
        valueLabel.numberOfLines = 0
        row.addArrangedSubview(keyLabel)
        row.addArrangedSubview(valueLabel)
        return row
    }

    private func sectionCard(title: String, body: String, isHeader: Bool) -> UIView {
        let wrapper = UIStackView()
        wrapper.axis = .vertical
        wrapper.spacing = 6
        let titleLabel = UILabel()
        titleLabel.font = UIFont.systemFont(ofSize: 16, weight: .semibold)
        titleLabel.textColor = InspectorTheme.textPrimary(isDark)
        titleLabel.text = title

        let card = UIView()
        card.backgroundColor = InspectorTheme.cardBackground(isDark)
        card.layer.cornerRadius = 14
        let bodyLabel = UILabel()
        bodyLabel.numberOfLines = 0
        bodyLabel.font = UIFont.systemFont(ofSize: 13, weight: .regular)
        bodyLabel.textColor = InspectorTheme.textPrimary(isDark)
        if body.isEmpty {
            bodyLabel.text = "-"
        } else if isHeader {
            bodyLabel.attributedText = attributedHeaders(body)
        } else {
            bodyLabel.text = body
        }
        bodyLabel.translatesAutoresizingMaskIntoConstraints = false
        card.addSubview(bodyLabel)
        NSLayoutConstraint.activate([
            bodyLabel.topAnchor.constraint(equalTo: card.topAnchor, constant: 12),
            bodyLabel.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 12),
            bodyLabel.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -12),
            bodyLabel.bottomAnchor.constraint(equalTo: card.bottomAnchor, constant: -12)
        ])

        wrapper.addArrangedSubview(titleLabel)
        wrapper.addArrangedSubview(card)
        return wrapper
    }

    private func prettyJsonString(_ value: Any) -> String? {
        guard JSONSerialization.isValidJSONObject(value),
              let data = try? JSONSerialization.data(withJSONObject: value, options: [.prettyPrinted]) else {
            return nil
        }
        return String(data: data, encoding: .utf8)
    }

    private func attributedHeaders(_ body: String) -> NSAttributedString {
        let result = NSMutableAttributedString()
        let lines = body.split(separator: "\n", omittingEmptySubsequences: false)
        let baseFont = UIFont.systemFont(ofSize: 13, weight: .regular)
        let boldFont = UIFont.systemFont(ofSize: 13, weight: .semibold)
        let baseColor = InspectorTheme.textPrimary(isDark)
        for (index, line) in lines.enumerated() {
            let lineString = String(line)
            if let range = lineString.range(of: ":") {
                let key = String(lineString[..<range.lowerBound])
                let value = String(lineString[range.lowerBound...])
                result.append(NSAttributedString(string: key, attributes: [.font: boldFont, .foregroundColor: baseColor]))
                result.append(NSAttributedString(string: value, attributes: [.font: baseFont, .foregroundColor: baseColor]))
            } else {
                result.append(NSAttributedString(string: lineString, attributes: [.font: baseFont, .foregroundColor: baseColor]))
            }
            if index < lines.count - 1 {
                result.append(NSAttributedString(string: "\n", attributes: [.font: baseFont, .foregroundColor: baseColor]))
            }
        }
        return result
    }

    private func formatHeaders(_ headersJson: String?) -> String {
        guard let headersJson = headersJson, !headersJson.isEmpty else { return "" }
        guard let data = headersJson.data(using: .utf8),
              let object = try? JSONSerialization.jsonObject(with: data, options: []) else {
            return headersJson
        }
        if let dict = object as? [String: Any] {
            return dict.keys.sorted().map { key in
                let value = dict[key] ?? ""
                return "\(key): \(value)"
            }.joined(separator: "\n")
        }
        return prettyJson(headersJson) ?? headersJson
    }

    private func formatBody(_ body: String?) -> String {
        guard let body = body, !body.isEmpty else { return "" }
        return prettyJson(body) ?? body
    }

    private func prettyJson(_ value: String) -> String? {
        guard let data = value.data(using: .utf8),
              let object = try? JSONSerialization.jsonObject(with: data, options: []),
              let pretty = try? JSONSerialization.data(withJSONObject: object, options: [.prettyPrinted]) else {
            return nil
        }
        return String(data: pretty, encoding: .utf8)
    }

    private func buildShareText() -> String {
        var lines: [String] = []
        lines.append(overviewText(for: log))
        let reqHeaders = formatHeaders(log.reqHeadersJson)
        let reqBody = formatBody(log.reqBody)
        let resHeaders = formatHeaders(log.resHeadersJson)
        let resBody = formatBody(log.resBody)
        if !reqHeaders.isEmpty { lines.append("\nRequest Headers:\n\(reqHeaders)") }
        if !reqBody.isEmpty { lines.append("\nRequest Body:\n\(reqBody)") }
        if !resHeaders.isEmpty { lines.append("\nResponse Headers:\n\(resHeaders)") }
        if !resBody.isEmpty { lines.append("\nResponse Body:\n\(resBody)") }
        return lines.joined(separator: "\n")
    }

    private func buildCurl() -> String {
        let method = log.method.isEmpty ? "GET" : log.method
        var parts: [String] = ["curl -X", method, "'\(log.url)'" ]
        if let headersJson = log.reqHeadersJson,
           let data = headersJson.data(using: .utf8),
           let object = try? JSONSerialization.jsonObject(with: data, options: []),
           let dict = object as? [String: Any] {
            dict.keys.sorted().forEach { key in
                let value = dict[key] ?? ""
                let header = "\(key): \(value)"
                parts.append("-H")
                parts.append("'\(escapeSingleQuotes(header))'")
            }
        }
        if let body = log.reqBody, !body.isEmpty {
            parts.append("--data")
            parts.append("'\(escapeSingleQuotes(body))'")
        }
        return parts.joined(separator: " ")
    }

    private func buildHar() -> String {
        let entry: [String: Any] = [
            "startedDateTime": ISO8601DateFormatter().string(from: Date(timeIntervalSince1970: TimeInterval(log.startTs) / 1000)),
            "time": log.durationMs ?? 0,
            "request": [
                "method": log.method,
                "url": log.url,
                "headers": formatHarHeaders(log.reqHeadersJson),
                "postData": log.reqBody ?? ""
            ],
            "response": [
                "status": log.resStatus ?? 0,
                "headers": formatHarHeaders(log.resHeadersJson),
                "content": log.resBody ?? ""
            ]
        ]
        let har: [String: Any] = ["log": ["version": "1.2", "creator": ["name": "capacitor-pica-network-logger", "version": "1"], "entries": [entry]]]
        return prettyJsonString(har) ?? har.description
    }

    private func formatHarHeaders(_ headersJson: String?) -> [[String: String]] {
        guard let headersJson = headersJson, !headersJson.isEmpty else { return [] }
        guard let data = headersJson.data(using: .utf8),
              let object = try? JSONSerialization.jsonObject(with: data, options: []),
              let dict = object as? [String: Any] else {
            return []
        }
        return dict.keys.sorted().map { key in
            ["name": key, "value": "\(dict[key] ?? "")"]
        }
    }

    private func escapeSingleQuotes(_ value: String) -> String {
        return value.replacingOccurrences(of: "'", with: "\\'")
    }

    private func shareText(_ text: String) {
        let controller = UIActivityViewController(activityItems: [text], applicationActivities: nil)
        present(controller, animated: true)
    }

    private func overviewText(for log: LogEntry) -> String {
        var lines: [String] = []
        if let status = log.resStatus { lines.append("Status: \(status)") }
        lines.append("Method: \(log.method)")
        lines.append("URL: \(log.url)")
        if let duration = log.durationMs { lines.append("Duration: \(duration)ms") }
        if let proto = log.protocol { lines.append("Protocol: \(proto)") }
        lines.append("SSL: \(log.ssl ? "Yes" : "No")")
        if log.error { lines.append("Error: \(log.errorMessage ?? "Unknown")") }
        return lines.joined(separator: "\n")
    }
}

enum InspectorTheme {
    static func background(_ dark: Bool) -> UIColor {
        return dark ? UIColor(red: 0.06, green: 0.07, blue: 0.07, alpha: 1) : UIColor(red: 0.96, green: 0.97, blue: 0.97, alpha: 1)
    }

    static func cardBackground(_ dark: Bool) -> UIColor {
        return dark ? UIColor(red: 0.12, green: 0.14, blue: 0.16, alpha: 1) : UIColor(red: 0.90, green: 0.93, blue: 0.92, alpha: 1)
    }

    static func textPrimary(_ dark: Bool) -> UIColor {
        return dark ? UIColor(red: 0.90, green: 0.91, blue: 0.91, alpha: 1) : UIColor(red: 0.10, green: 0.11, blue: 0.12, alpha: 1)
    }

    static func textSecondary(_ dark: Bool) -> UIColor {
        return dark ? UIColor(red: 0.71, green: 0.74, blue: 0.76, alpha: 1) : UIColor(red: 0.29, green: 0.34, blue: 0.32, alpha: 1)
    }

    static func statusColor(_ status: Int64?) -> UIColor {
        guard let status = status else {
            return UIColor(red: 0.36, green: 0.38, blue: 0.44, alpha: 1)
        }
        switch status {
        case 200...299:
            return UIColor(red: 0.12, green: 0.48, blue: 0.36, alpha: 1)
        case 300...399:
            return UIColor(red: 0.16, green: 0.44, blue: 0.69, alpha: 1)
        case 400...499:
            return UIColor(red: 0.69, green: 0.42, blue: 0.10, alpha: 1)
        case 500...599:
            return UIColor(red: 0.70, green: 0.22, blue: 0.22, alpha: 1)
        default:
            return UIColor(red: 0.18, green: 0.19, blue: 0.25, alpha: 1)
        }
    }

    static func methodColor(_ method: String) -> UIColor {
        switch method.uppercased() {
        case "GET":
            return UIColor(red: 0.29, green: 0.24, blue: 0.60, alpha: 1)
        case "POST":
            return UIColor(red: 0.48, green: 0.18, blue: 0.49, alpha: 1)
        case "PUT":
            return UIColor(red: 0.05, green: 0.44, blue: 0.51, alpha: 1)
        case "PATCH":
            return UIColor(red: 0.41, green: 0.36, blue: 0.10, alpha: 1)
        case "DELETE":
            return UIColor(red: 0.36, green: 0.16, blue: 0.16, alpha: 1)
        case "HEAD":
            return UIColor(red: 0.24, green: 0.29, blue: 0.42, alpha: 1)
        case "OPTIONS":
            return UIColor(red: 0.29, green: 0.29, blue: 0.29, alpha: 1)
        default:
            return UIColor(red: 0.24, green: 0.29, blue: 0.35, alpha: 1)
        }
    }

    static func contentColor(for background: UIColor) -> UIColor {
        var white: CGFloat = 0
        background.getWhite(&white, alpha: nil)
        return white < 0.55 ? .white : .black
    }

    static func formatTime(_ epochMillis: Int64) -> String {
        let date = Date(timeIntervalSince1970: TimeInterval(epochMillis) / 1000)
        let formatter = DateFormatter()
        formatter.dateFormat = "dd MMM HH:mm:ss"
        return formatter.string(from: date)
    }

    static func formatSize(_ bytes: Int) -> String {
        if bytes <= 0 { return "-" }
        let kb = Double(bytes) / 1024.0
        if kb < 1024 {
            return String(format: "%.2f KB", kb)
        }
        return String(format: "%.2f MB", kb / 1024.0)
    }
}
#endif
