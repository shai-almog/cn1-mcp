#!/usr/bin/env python3
from __future__ import annotations

import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Dict, Optional

ROOT = Path(__file__).resolve().parents[2]
TARGET_DIR = ROOT / "target"
REPORT_PATH = ROOT / "quality-report.md"


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


def parse_spotbugs() -> Optional[Dict[str, int]]:
    report = TARGET_DIR / "spotbugsXml.xml"
    if not report.exists():
        return None
    try:
        root = ET.parse(report).getroot()
    except ET.ParseError:
        return None
    severities = {"High": 0, "Normal": 0, "Low": 0}
    found = False
    for bug in root.findall("BugInstance"):
        found = True
        priority = bug.attrib.get("priority")
        if priority == "1":
            severities["High"] += 1
        elif priority == "2":
            severities["Normal"] += 1
        else:
            severities["Low"] += 1
    return severities if found else None


def parse_pmd() -> Optional[Dict[str, int]]:
    report = TARGET_DIR / "pmd.xml"
    if not report.exists():
        return None
    try:
        root = ET.parse(report).getroot()
    except ET.ParseError:
        return None
    priority_counts = {"1": 0, "2": 0, "3": 0, "4": 0, "5": 0}
    found = False
    for violation in root.iter():
        if not violation.tag.endswith("violation"):
            continue
        found = True
        priority = violation.attrib.get("priority", "")
        if priority in priority_counts:
            priority_counts[priority] += 1
        else:
            priority_counts["5"] += 1
    return priority_counts if found else None


def parse_checkstyle() -> Optional[Dict[str, int]]:
    report = TARGET_DIR / "checkstyle-result.xml"
    if not report.exists():
        return None
    try:
        root = ET.parse(report).getroot()
    except ET.ParseError:
        return None
    severities = {"error": 0, "warning": 0, "info": 0}
    found = False
    for error in root.findall(".//error"):
        found = True
        severity = error.attrib.get("severity", "warning").lower()
        if severity in severities:
            severities[severity] += 1
        else:
            severities["warning"] += 1
    return severities if found else None


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


def format_spotbugs(data: Optional[Dict[str, int]]) -> str:
    if not data:
        return "- ⚠️ SpotBugs report not generated."
    total = sum(data.values())
    status = "✅" if total == 0 else "❌"
    breakdown = ", ".join(f"{sev}: {count}" for sev, count in data.items() if count > 0)
    breakdown = breakdown or "no issues"
    return f"- {status} **SpotBugs:** {total} findings ({breakdown})"


def format_pmd(data: Optional[Dict[str, int]]) -> str:
    if not data:
        return "- ⚠️ PMD report not generated."
    total = sum(data.values())
    status = "✅" if total == 0 else "❌"
    breakdown = ", ".join(
        f"P{priority}: {count}" for priority, count in sorted(data.items()) if count > 0
    )
    breakdown = breakdown or "no issues"
    return f"- {status} **PMD:** {total} findings ({breakdown})"


def format_checkstyle(data: Optional[Dict[str, int]]) -> str:
    if not data:
        return "- ⚠️ Checkstyle report not generated."
    total = sum(data.values())
    status = "✅" if total == 0 else "❌"
    breakdown = ", ".join(
        f"{severity.title()}: {count}" for severity, count in data.items() if count > 0
    )
    breakdown = breakdown or "no issues"
    return f"- {status} **Checkstyle:** {total} findings ({breakdown})"


def build_report() -> str:
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
        format_spotbugs(spotbugs),
        format_pmd(pmd),
        format_checkstyle(checkstyle),
        "",
        "_Generated automatically by the MCP CI workflow._",
    ]
    return "\n".join(lines)


def main() -> None:
    report = build_report()
    REPORT_PATH.write_text(report + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
