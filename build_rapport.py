# -*- coding: utf-8 -*-
"""Convertit rapport.md en HTML puis en PDF via Edge headless."""
import html
import re
import subprocess
import tempfile
from pathlib import Path

BASE = Path(__file__).resolve().parent
SRC = BASE / "rapport.md"
HTML = BASE / "rapport.html"
PDF = BASE / "rapport.pdf"


def inline(text: str) -> str:
    """Échappe le HTML puis applique le gras et le code en ligne."""
    text = html.escape(text, quote=False)
    text = re.sub(r"`([^`]+)`", r"<code>\1</code>", text)
    text = re.sub(r"\*\*(.+?)\*\*", r"<strong>\1</strong>", text)
    return text


def split_row(line: str):
    r"""Découpe une ligne de tableau en respectant les pipes échappés \| et \*."""
    s = line.strip()
    if s.startswith("|"):
        s = s[1:]
    if s.endswith("|") and not s.endswith("\\|"):
        s = s[:-1]
    cells, cur, i = [], "", 0
    while i < len(s):
        c = s[i]
        if c == "\\" and i + 1 < len(s) and s[i + 1] in "|*":
            cur += s[i + 1]
            i += 2
            continue
        if c == "|":
            cells.append(cur.strip())
            cur = ""
            i += 1
            continue
        cur += c
        i += 1
    cells.append(cur.strip())
    return cells


SEP = re.compile(r"^\s*\|?[\s:\-|]+\|?\s*$")


def convert(markdown: str) -> str:
    lines = markdown.splitlines()
    body = []
    para = []
    i = 0

    def flush():
        if para:
            body.append("<p>" + inline(" ".join(para)) + "</p>")
            para.clear()

    while i < len(lines):
        line = lines[i]
        s = line.strip()

        if not s:
            flush()
            i += 1
            continue

        if s == "---":
            flush()
            i += 1
            continue

        if s.startswith("IMG|"):
            flush()
            parts = s.split("|", 2)
            path = parts[1].strip() if len(parts) > 1 else ""
            legend = parts[2].strip() if len(parts) > 2 else ""
            if (BASE / path).is_file():
                picture = '<img class="auto" src="' + html.escape(path, quote=True) + '">'
            else:
                picture = ('<div class="imgph">'
                           + inline("IMAGE À INSÉRER : " + path) + "</div>")
            body.append('<div class="imgbox">' + picture
                        + '<div class="legend">' + inline(legend)
                        + "</div></div>")
            i += 1
            continue

        if s.startswith("```"):
            flush()
            i += 1
            code = []
            while i < len(lines) and not lines[i].strip().startswith("```"):
                code.append(lines[i])
                i += 1
            i += 1
            body.append("<pre><code>" + html.escape("\n".join(code), quote=False) + "</code></pre>")
            continue

        if s.startswith("|") and i + 1 < len(lines) and SEP.match(lines[i + 1]) and "-" in lines[i + 1]:
            flush()
            headers = split_row(s)
            i += 2
            rows = []
            while i < len(lines) and lines[i].strip().startswith("|"):
                rows.append(split_row(lines[i]))
                i += 1
            cls = ' class="wide"' if len(headers) > 6 else ""
            body.append("<table" + cls + "><thead><tr>"
                        + "".join("<th>" + inline(c) + "</th>" for c in headers)
                        + "</tr></thead><tbody>")
            for row in rows:
                body.append("<tr>" + "".join("<td>" + inline(c) + "</td>" for c in row) + "</tr>")
            body.append("</tbody></table>")
            continue

        m = re.match(r"^(#{1,3})\s+(.*)$", s)
        if m:
            flush()
            level = len(m.group(1))
            title = m.group(2)
            cls = ' class="newsec"' if level == 2 and i > 0 else ""
            body.append(f"<h{level}{cls}>" + inline(title) + f"</h{level}>")
            i += 1
            continue

        if s.startswith("> "):
            flush()
            quote = []
            while i < len(lines) and lines[i].strip().startswith(">"):
                quote.append(lines[i].strip().lstrip(">").strip())
                i += 1
            body.append("<blockquote>" + inline(" ".join(quote)) + "</blockquote>")
            continue

        if re.match(r"^\d+\.\s+", s):
            flush()
            items = []
            while i < len(lines) and re.match(r"^\d+\.\s+", lines[i].strip()):
                items.append(re.sub(r"^\d+\.\s+", "", lines[i].strip()))
                i += 1
            body.append("<ol>" + "".join("<li>" + inline(x) + "</li>" for x in items) + "</ol>")
            continue

        if s.startswith("- "):
            flush()
            items = []
            while i < len(lines) and lines[i].strip().startswith("- "):
                items.append(lines[i].strip()[2:])
                i += 1
            body.append("<ul>" + "".join("<li>" + inline(x) + "</li>" for x in items) + "</ul>")
            continue

        para.append(s)
        i += 1

    flush()
    return "\n".join(body)


CSS = """
@page { size: A4; margin: 12mm 13mm 14mm 13mm; }
* { box-sizing: border-box; }
body {
  font-family: "Segoe UI", "DejaVu Sans", Arial, sans-serif;
  font-size: 8.6pt; line-height: 1.32; color: #1b1b1b; margin: 0;
}
h1 { font-size: 16pt; color: #24304a; border-bottom: 2px solid #46547a;
     padding-bottom: 4px; margin: 0 0 8px; }
h2 { font-size: 11.5pt; color: #24304a; margin: 10px 0 5px;
     border-bottom: 1px solid #b8c0d2; padding-bottom: 2px; }
h2.newsec { margin-top: 10px; }
h3 { font-size: 9.6pt; color: #33415f; margin: 8px 0 4px; }
p { margin: 4px 0; text-align: justify; }
ul, ol { margin: 4px 0 6px 16px; padding-left: 6px; }
li { margin: 1px 0; }
blockquote {
  border-left: 3px solid #5b6b93; background: #f2f4fa;
  padding: 5px 9px; margin: 7px 0; font-size: 8.2pt;
}
code { font-family: Consolas, "Courier New", monospace; font-size: 8pt; }
p code, li code, td code, th code, blockquote code { background: #eef0f5; padding: 0 2px; }
pre {
  background: #f6f7fa; border: 1px solid #cfd4e0; border-left: 3px solid #5b6b93;
  padding: 6px 8px; margin: 6px 0 8px; white-space: pre-wrap;
  font-size: 7.1pt; line-height: 1.24;
}
pre code { font-size: inherit; background: none; padding: 0; }
table { width: 100%; border-collapse: collapse; margin: 6px 0 9px; font-size: 7.8pt; }
table.wide { font-size: 6.6pt; }
th, td { border: 1px solid #9aa3b8; padding: 2px 4px; text-align: left; vertical-align: top; }
th { background: #e7eaf2; color: #24304a; font-weight: 600; }
tr, pre, blockquote, .imgbox { page-break-inside: avoid; }
.imgbox { margin: 7px 0 10px; }
.imgph {
  border: 2px dashed #7b89ad; background: #f7f8fc; color: #40507a;
  text-align: center; padding: 16px 8px; font-size: 8pt; font-weight: 600;
}
.imgbox img.auto {
  display: block; margin: 0 auto; max-width: 100%; max-height: 52mm;
  border: 1px solid #cfd4e0;
}
.legend { font-size: 7.8pt; color: #555; text-align: center; margin-top: 3px; }
"""


def page_count(data: bytes) -> int:
    return len(re.findall(rb"/Type\s*/Page\b", data))


def main():
    content = SRC.read_text(encoding="utf-8")
    doc = ("<!DOCTYPE html>\n<html lang=\"fr\"><head><meta charset=\"utf-8\">"
           "<title>Rapport — Expressions régulières et automates</title><style>" + CSS +
           "</style></head><body>" + convert(content) + "</body></html>")
    HTML.write_text(doc, encoding="utf-8")

    edge = Path(r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe")
    if not edge.exists():
        raise SystemExit("Microsoft Edge introuvable : " + str(edge))
    profile = Path(tempfile.gettempdir()) / "opencode-edge-rapport"
    common = [
        str(edge), "--disable-gpu", "--disable-extensions", "--hide-scrollbars",
        "--no-pdf-header-footer", "--print-to-pdf-no-header",
        "--user-data-dir=" + str(profile),
        "--virtual-time-budget=4000",
        f"--print-to-pdf={PDF.resolve()}",
        HTML.resolve().as_uri(),
    ]
    for headless in ("--headless=new", "--headless"):
        try:
            subprocess.run([common[0], headless] + common[1:],
                           check=False, timeout=120)
        except subprocess.TimeoutExpired:
            continue
        if PDF.exists() and PDF.stat().st_size > 1000:
            data = PDF.read_bytes()
            print("PDF OK :", PDF.resolve(), PDF.stat().st_size, "octets,",
                  page_count(data), "pages")
            return
        print("Tentative", headless, "échouée")
    raise SystemExit("Échec de la génération du PDF")


if __name__ == "__main__":
    main()
