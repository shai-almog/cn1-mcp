#!/usr/bin/env python3
from __future__ import annotations

import os
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, List, Optional

ROOT = Path(__file__).resolve().parents[2]
TARGET_DIR = ROOT / "target"
REPORT_PATH = ROOT / "quality-report.md"


@dataclass
class Finding:
    severity: str
    location: str
    message: str
    rule: Optional[str] = None

    def to_markdown(self) -> str:
        rule_suffix = f" _(rule: `{self.rule}`)_" if self.rule else ""
        return f"{self.severity}: `{self.location}` – {self.message}{rule_suffix}"


@dataclass
class AnalysisReport:
    totals: Dict[str, int]
    findings: List[Finding]


def _clean_message(value: Optional[str]) -> str:
    if not value:
        return ""
    return " ".join(value.split())


def _relative_path(raw_path: Optional[str]) -> str:
    if not raw_path:
        return "Unknown location"
    try:
        return str(Path(raw_path).resolve().relative_to(ROOT))
    except ValueError:
        return raw_path.replace("\\", "/")


def parse_surefire() -> Optional[Dict[str, int]]:
    reports_dir = TARGET_DIR / "surefire-reports"
    if not reports_dir.exists():
        return None
    totals = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0}
    found = False
    for report in reports_dir.glob("TEST-*.xml"):
        try:
            tree = ET.parse(report)
            root = tree.getroot()
        except ET.ParseError:
            continue
        found = True
        for key in totals:
            totals[key] += int(float(root.attrib.get(key, "0")))
    return totals if found else None


def parse_jacoco() -> Optional[float]:
    report = TARGET_DIR / "site" / "jacoco" / "jacoco.xml"
    if not report.exists():
        return None
    try:
        root = ET.parse(report).getroot()
    except ET.ParseError:
        return None
    covered = 0
    missed = 0
    for counter in root.findall("counter"):
        if counter.attrib.get("type") == "LINE":
            covered += int(counter.attrib.get("covered", "0"))
            missed += int(counter.attrib.get("missed", "0"))
    total = covered + missed
    if total == 0:
        return None
    return covered / total * 100.0


def parse_spotbugs() -> Optional[AnalysisReport]:
    report: Optional[Path] = None
    for candidate in ("spotbugsXml.xml", "spotbugs.xml"):
        path = TARGET_DIR / candidate
        if path.exists():
            report = path
            break
    if report is None:
        return None
    try:
        root = ET.parse(report).getroot()
    except ET.ParseError:
        return None
    severities = {"High": 0, "Normal": 0, "Low": 0}
    findings: List[Finding] = []
    bug_instances = root.findall("BugInstance")
    if bug_instances:
        for bug in bug_instances:
            priority = bug.attrib.get("priority")
            if priority == "1":
                severity = "High"
            elif priority == "2":
                severity = "Normal"
            else:
                severity = "Low"
            severities[severity] += 1
            message = _clean_message(
                bug.findtext("ShortMessage")
                or bug.findtext("LongMessage")
                or bug.attrib.get("message")
            )
            bug_type = bug.attrib.get("type")
            source_line = bug.find(".//SourceLine[@primary='true']")
            if source_line is None:
                source_line = bug.find(".//SourceLine")
            if source_line is not None:
                source_path = source_line.attrib.get("sourcepath")
                line = source_line.attrib.get("start") or source_line.attrib.get("end")
            else:
                source_path = None
                line = None
            if source_path:
                location = _relative_path(source_path)
                if line:
                    location = f"{location}:{line}"
            else:
                class_elem = bug.find("Class")
                class_name = class_elem.attrib.get("classname") if class_elem is not None else bug.attrib.get("type")
                location = class_name or "Unknown"
                if line:
                    location = f"{location}:{line}"
            findings.append(
                Finding(
                    severity=severity,
                    location=location,
                    message=message or bug_type or "Issue detected",
                    rule=bug_type,
                )
            )
    else:
        for file_elem in root.findall("file"):
            class_name = file_elem.attrib.get("classname") or file_elem.attrib.get("name")
            source_path = file_elem.attrib.get("sourcepath") or file_elem.attrib.get("name")
            if not source_path and class_name:
                source_path = class_name.replace(".", "/") + ".java"
            for bug in file_elem.findall("BugInstance"):
                priority = bug.attrib.get("priority")
                if priority == "1":
                    severity = "High"
                elif priority == "2":
                    severity = "Normal"
                else:
                    severity = "Low"
                severities[severity] += 1
                message = _clean_message(
                    bug.findtext("ShortMessage")
                    or bug.findtext("LongMessage")
                    or bug.attrib.get("message")
                )
                bug_type = bug.attrib.get("type")
                line = bug.attrib.get("lineNumber")
                location_base = _relative_path(source_path) if source_path else class_name or "Unknown"
                if line:
                    location = f"{location_base}:{line}"
                else:
                    location = location_base
                findings.append(
                    Finding(
                        severity=severity,
                        location=location,
                        message=message or bug_type or "Issue detected",
                        rule=bug_type,
                    )
                )
    if not findings:
        return None
    severity_order = {"High": 0, "Normal": 1, "Low": 2}
    findings.sort(key=lambda item: (severity_order.get(item.severity, 99)))
    return AnalysisReport(totals=severities, findings=findings)


def parse_pmd() -> Optional[AnalysisReport]:
    report = TARGET_DIR / "pmd.xml"
    if not report.exists():
        return None
    try:
        root = ET.parse(report).getroot()
    except ET.ParseError:
        return None
    priority_counts = {"1": 0, "2": 0, "3": 0, "4": 0, "5": 0}
    findings: List[Finding] = []
    for file_elem in root.iter():
        if not file_elem.tag.endswith("file"):
            continue
        file_path = file_elem.attrib.get("name")
        for violation in file_elem.iter():
            if not violation.tag.endswith("violation"):
                continue
            priority = violation.attrib.get("priority", "5")
            priority_counts[priority] = priority_counts.get(priority, 0) + 1
            location = _relative_path(file_path)
            begin_line = violation.attrib.get("beginline") or violation.attrib.get("line")
            if begin_line:
                location = f"{location}:{begin_line}"
            rule = violation.attrib.get("rule") or violation.attrib.get("ruleset")
            message = _clean_message(violation.text)
            findings.append(
                Finding(
                    severity=f"P{priority}",
                    location=location,
                    message=message or "Violation detected",
                    rule=rule,
                )
            )
    findings = [finding for finding in findings if finding.message]
    if not findings:
        return None
    findings.sort(key=lambda item: int(item.severity[1:]) if item.severity[1:].isdigit() else 99)
    return AnalysisReport(totals=priority_counts, findings=findings)


def parse_checkstyle() -> Optional[AnalysisReport]:
    report = TARGET_DIR / "checkstyle-result.xml"
    if not report.exists():
        return None
    try:
        root = ET.parse(report).getroot()
    except ET.ParseError:
        return None
    severities = {"error": 0, "warning": 0, "info": 0}
    findings: List[Finding] = []
    for file_elem in root.findall("file"):
        file_path = file_elem.attrib.get("name")
        for error in file_elem.findall("error"):
            severity = error.attrib.get("severity", "warning").lower()
            if severity in severities:
                severities[severity] += 1
            else:
                severities["warning"] += 1
            message = _clean_message(error.attrib.get("message"))
            source = error.attrib.get("source")
            line = error.attrib.get("line")
            column = error.attrib.get("column")
            location = _relative_path(file_path)
            if line:
                location = f"{location}:{line}"
                if column:
                    location = f"{location}:{column}"
            findings.append(
                Finding(
                    severity=severity.title(),
                    location=location,
                    message=message or "Checkstyle violation",
                    rule=source.split(".")[-1] if source else None,
                )
            )
    findings = [finding for finding in findings if finding.message]
    if not findings:
        return None
    severity_order = {"Error": 0, "Warning": 1, "Info": 2}
    findings.sort(key=lambda item: severity_order.get(item.severity, 99))
    return AnalysisReport(totals=severities, findings=findings)


def format_tests(totals: Optional[Dict[str, int]]) -> str:
    if not totals:
        return "- ⚠️ No test results were found."
    failed = totals["failures"] + totals["errors"]
    status = "✅" if failed == 0 else "❌"
    return (
        f"- {status} **Tests:** {totals['tests']} total, {failed} failed, "
        f"{totals['skipped']} skipped"
    )


def format_coverage(coverage: Optional[float]) -> str:
    if coverage is None:
        return "- ⚠️ Coverage report not generated."
    return f"- 📊 **Line coverage:** {coverage:.2f}%"


def format_spotbugs(data: Optional[AnalysisReport], artifact_url: Optional[str]) -> List[str]:
    if not data:
        return ["- ⚠️ SpotBugs report not generated."]
    total = sum(data.totals.values())
    status = "✅" if total == 0 else "❌"
    breakdown = ", ".join(
        f"{sev}: {count}" for sev, count in data.totals.items() if count > 0
    )
    breakdown = breakdown or "no issues"
    link_suffix = f" [[Full report]]({artifact_url})" if artifact_url else ""
    lines = [f"- {status} **SpotBugs:** {total} findings ({breakdown}){link_suffix}"]
    highlights = data.findings[:5]
    if highlights:
        lines.append("  <details>")
        lines.append("  <summary>Top findings</summary>")
        lines.extend(f"  - {finding.to_markdown()}" for finding in highlights)
        if total > len(highlights):
            lines.append(f"  - …and {total - len(highlights)} more")
        lines.append("  </details>")
    return lines


def format_pmd(data: Optional[AnalysisReport], artifact_url: Optional[str]) -> List[str]:
    if not data:
        return ["- ⚠️ PMD report not generated."]
    total = sum(data.totals.values())
    status = "✅" if total == 0 else "❌"
    breakdown = ", ".join(
        f"P{priority}: {count}" for priority, count in sorted(data.totals.items()) if count > 0
    )
    breakdown = breakdown or "no issues"
    link_suffix = f" [[Full report]]({artifact_url})" if artifact_url else ""
    lines = [f"- {status} **PMD:** {total} findings ({breakdown}){link_suffix}"]
    highlights = data.findings[:5]
    if highlights:
        lines.append("  <details>")
        lines.append("  <summary>Top findings</summary>")
        lines.extend(f"  - {finding.to_markdown()}" for finding in highlights)
        if total > len(highlights):
            lines.append(f"  - …and {total - len(highlights)} more")
        lines.append("  </details>")
    return lines


def format_checkstyle(data: Optional[AnalysisReport], artifact_url: Optional[str]) -> List[str]:
    if not data:
        return ["- ⚠️ Checkstyle report not generated."]
    total = sum(data.totals.values())
    status = "✅" if total == 0 else "❌"
    breakdown = ", ".join(
        f"{severity.title()}: {count}" for severity, count in data.totals.items() if count > 0
    )
    breakdown = breakdown or "no issues"
    link_suffix = f" [[Full report]]({artifact_url})" if artifact_url else ""
    lines = [f"- {status} **Checkstyle:** {total} findings ({breakdown}){link_suffix}"]
    highlights = data.findings[:5]
    if highlights:
        lines.append("  <details>")
        lines.append("  <summary>Top findings</summary>")
        lines.extend(f"  - {finding.to_markdown()}" for finding in highlights)
        if total > len(highlights):
            lines.append(f"  - …and {total - len(highlights)} more")
        lines.append("  </details>")
    return lines


def build_report(artifact_url: Optional[str]) -> str:
    tests = parse_surefire()
    coverage = parse_jacoco()
    spotbugs = parse_spotbugs()
    pmd = parse_pmd()
    checkstyle = parse_checkstyle()

    lines = [
        "## ✅ Continuous Quality Report",
        "",
        "### Test & Coverage",
        format_tests(tests),
        format_coverage(coverage),
        "",
        "### Static Analysis",
    ]
    for block in (
        format_spotbugs(spotbugs, artifact_url),
        format_pmd(pmd, artifact_url),
        format_checkstyle(checkstyle, artifact_url),
    ):
        lines.extend(block)
    lines.extend(
        [
            "",
            "_Generated automatically by the MCP CI workflow._",
        ]
    )
    return "\n".join(lines)


def main() -> None:
    artifact_url = os.environ.get("STATIC_ANALYSIS_ARTIFACT_URL")
    report = build_report(artifact_url if artifact_url else None)
    REPORT_PATH.write_text(report + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
