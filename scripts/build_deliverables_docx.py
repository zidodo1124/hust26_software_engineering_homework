"""Build the Word (.docx) versions of deliverables/*.md.

Formatting follows the course report sample: A4, margins 2.54/3.17 cm, body 宋体小四 with 1.5 line
spacing, headings in 黑体, table captions above tables and figure captions below figures (黑体小四),
table text 宋体五号, one chapter per page.

Requirements: pandoc (PANDOC env var or on PATH) and python-docx.
Usage (from the repository root):
    python scripts/build_deliverables_docx.py
"""
from __future__ import annotations

import copy
import os
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_LINE_SPACING, WD_BREAK
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Pt, RGBColor

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "deliverables"
OUT = SRC / "word"
PANDOC = os.environ.get("PANDOC") or shutil.which("pandoc") or "pandoc"
HEADER_TEXT = "华中科技大学计算机科学与技术学院 · 软件工程课程设计 · HUSTer's DDL Saver"

SONG, HEI, MONO = "宋体", "黑体", "Consolas"


def insert_ordered(parent, element, successors: set[str]) -> None:
    """Insert element before the first existing child that the OOXML schema orders after it."""
    anchor = next((child for child in parent if child.tag.split("}")[-1] in successors), None)
    if anchor is not None:
        anchor.addprevious(element)
    else:
        parent.append(element)


TBL_AFTER_BORDERS = {"shd", "tblLayout", "tblCellMar", "tblLook", "tblCaption", "tblDescription"}
TBL_AFTER_LAYOUT = {"tblCellMar", "tblLook", "tblCaption", "tblDescription"}
PPR_AFTER_BORDER = {"shd", "tabs", "suppressAutoHyphens", "kinsoku", "wordWrap", "overflowPunct", "topLinePunct",
                    "autoSpaceDE", "autoSpaceDN", "bidi", "adjustRightInd", "snapToGrid", "spacing", "ind",
                    "contextualSpacing", "mirrorIndents", "suppressOverlap", "jc", "textDirection", "textAlignment",
                    "textboxTightWrap", "outlineLvl", "divId", "cnfStyle", "rPr", "sectPr", "pPrChange"}


def set_font(style_or_run, east_asia: str, size_pt: float | None = None, bold: bool | None = None,
             latin: str = "Times New Roman", color: RGBColor | None = None) -> None:
    font = style_or_run.font
    font.name = latin
    if size_pt is not None:
        font.size = Pt(size_pt)
    if bold is not None:
        font.bold = bold
    if color is not None:
        font.color.rgb = color
    rpr = style_or_run.element.get_or_add_rPr()
    fonts = rpr.find(qn("w:rFonts"))
    if fonts is None:
        fonts = OxmlElement("w:rFonts")
        rpr.insert(0, fonts)
    for attr in ("w:ascii", "w:hAnsi", "w:cs"):
        fonts.set(qn(attr), latin)
    fonts.set(qn("w:eastAsia"), east_asia)
    for theme in ("w:asciiTheme", "w:hAnsiTheme", "w:eastAsiaTheme", "w:cstheme"):
        if fonts.get(qn(theme)) is not None:
            del fonts.attrib[qn(theme)]


def paragraph_style(doc, name, east_asia, size, bold=False, align=None, before=0, after=0,
                    line=None, keep_next=False, page_break=False, latin="Times New Roman", indent_first=None):
    try:
        style = doc.styles[name]
    except KeyError:
        style = doc.styles.add_style(name, 1)
    set_font(style, east_asia, size, bold, latin=latin, color=RGBColor(0, 0, 0))
    pf = style.paragraph_format
    pf.space_before, pf.space_after = Pt(before), Pt(after)
    if line is not None:
        pf.line_spacing_rule = WD_LINE_SPACING.MULTIPLE
        pf.line_spacing = line
    if align is not None:
        pf.alignment = align
    pf.keep_with_next = keep_next
    pf.page_break_before = page_break
    if indent_first is not None:
        pf.first_line_indent = indent_first
    return style


def build_reference(path: Path) -> None:
    subprocess.run([PANDOC, "-o", str(path), "--print-default-data-file", "reference.docx"], check=True)
    doc = Document(path)
    section = doc.sections[0]
    section.page_width, section.page_height = Cm(21.0), Cm(29.7)
    section.top_margin = section.bottom_margin = Cm(2.54)
    section.left_margin = section.right_margin = Cm(3.17)
    section.header_distance, section.footer_distance = Cm(1.5), Cm(1.75)
    section.gutter = Cm(0)

    center = WD_ALIGN_PARAGRAPH.CENTER
    paragraph_style(doc, "Normal", SONG, 12, line=1.5)
    for name in ("Body Text", "First Paragraph"):
        paragraph_style(doc, name, SONG, 12, line=1.5, before=0, after=4)
    paragraph_style(doc, "Compact", SONG, 10.5, line=1.15)                       # table cells and tight lists
    paragraph_style(doc, "Title", HEI, 22, bold=True, align=center, before=120, after=24)
    paragraph_style(doc, "Subtitle", HEI, 16, align=center, after=12)
    paragraph_style(doc, "Author", SONG, 14, align=center, after=6)
    paragraph_style(doc, "Date", SONG, 14, align=center, after=6)
    paragraph_style(doc, "Heading 1", HEI, 18, bold=True, align=center, before=12, after=12, keep_next=True, page_break=True)
    paragraph_style(doc, "Heading 2", HEI, 14, bold=True, before=12, after=6, keep_next=True)
    paragraph_style(doc, "Heading 3", HEI, 12, bold=True, before=8, after=4, keep_next=True)
    paragraph_style(doc, "Table Caption", HEI, 12, align=center, before=8, after=4, keep_next=True)
    paragraph_style(doc, "Image Caption", HEI, 12, align=center, before=2, after=10)
    paragraph_style(doc, "Captioned Figure", SONG, 12, align=center, before=6, keep_next=True)
    paragraph_style(doc, "Figure", SONG, 12, align=center, before=6, keep_next=True)
    paragraph_style(doc, "TOC Heading", HEI, 18, bold=True, align=center, after=12)
    paragraph_style(doc, "Toc Entry 1", SONG, 12, bold=True, line=1.5)
    paragraph_style(doc, "Toc Entry 2", SONG, 12, line=1.5, indent_first=Cm(0.84))
    paragraph_style(doc, "Source Code", MONO, 9, line=1.0, latin=MONO)
    try:
        set_font(doc.styles["Verbatim Char"], MONO, 9, latin=MONO)
    except KeyError:
        pass
    for level in (1, 2, 3):
        try:
            paragraph_style(doc, f"TOC {level}" if f"TOC {level}" in [s.name for s in doc.styles] else f"toc {level}",
                            SONG, 12, bold=(level == 1), line=1.3)
        except Exception:
            pass

    header = section.header.paragraphs[0]
    header.text = HEADER_TEXT
    header.alignment = center
    for run in header.runs:
        set_font(run, SONG, 9)
    bottom = OxmlElement("w:pBdr")
    line = OxmlElement("w:bottom")
    for key, value in (("w:val", "single"), ("w:sz", "4"), ("w:space", "1"), ("w:color", "808080")):
        line.set(qn(key), value)
    bottom.append(line)
    insert_ordered(header._p.get_or_add_pPr(), bottom, PPR_AFTER_BORDER)

    footer = section.footer.paragraphs[0]
    footer.alignment = center
    run = footer.add_run()
    for kind, text in (("begin", None), (None, "PAGE"), ("end", None)):
        if kind:
            fld = OxmlElement("w:fldChar")
            fld.set(qn("w:fldCharType"), kind)
            run._r.append(fld)
        else:
            instr = OxmlElement("w:instrText")
            instr.set(qn("xml:space"), "preserve")
            instr.text = text
            run._r.append(instr)
    set_font(run, SONG, 10.5)
    doc.save(path)


TABLE_CAPTION = re.compile(r"^表\d+-\d+\s")
IMAGE = re.compile(r"^!\[(?P<alt>[^\]]*)\]\((?P<src>[^)]+)\)\s*$")


def image_width(src: str, base: Path) -> str:
    if "screenshots/" in src:
        return "6.2cm"                                   # phone screenshots: narrow portrait
    try:
        from PIL import Image
        with Image.open(base / src) as im:
            ratio = im.height / im.width
    except Exception:
        return "14.5cm"
    if ratio > 1.6:
        return "7.5cm"
    if ratio > 1.0:
        return "11cm"
    return "14.5cm"


def static_toc(text: str) -> list[str]:
    """A plain table of contents (chapters and sections) that every Word / WPS / LibreOffice shows as-is."""
    lines, in_code = ['::: {custom-style="TOC Heading"}', "目录", ":::", ""], False
    for line in text.splitlines():
        if line.startswith("```"):
            in_code = not in_code
        if in_code:
            continue
        if line.startswith("# ") or line.startswith("## "):
            level = 1 if line.startswith("# ") else 2
            lines += [f'::: {{custom-style="Toc Entry {level}"}}', line.lstrip("#").strip(), ":::", ""]
    return lines


def preprocess(text: str, base: Path) -> str:
    out = []
    # The table of contents is built from the body only (after the YAML title block).
    body = text.split("\n---\n", 1)[1] if text.startswith("---\n") else text
    toc_done = False
    for line in text.splitlines():
        if not toc_done and line.startswith("# "):
            out += static_toc(body)
            toc_done = True
        if TABLE_CAPTION.match(line):
            out += ['::: {custom-style="Table Caption"}', line, ":::"]
            continue
        match = IMAGE.match(line)
        if match:
            out.append(f"{line.rstrip()}{{width={image_width(match['src'], base)}}}")
            continue
        out.append(line)
    return "\n".join(out) + "\n"


def border(tag: str) -> OxmlElement:
    element = OxmlElement(f"w:{tag}")
    for key, value in (("w:val", "single"), ("w:sz", "4"), ("w:space", "0"), ("w:color", "404040")):
        element.set(qn(key), value)
    return element


def postprocess(path: Path) -> None:
    doc = Document(path)
    for table in doc.tables:
        table.alignment = WD_TABLE_ALIGNMENT.CENTER
        tbl_pr = table._tbl.tblPr
        for old in tbl_pr.findall(qn("w:tblBorders")):
            tbl_pr.remove(old)
        borders = OxmlElement("w:tblBorders")
        for tag in ("top", "left", "bottom", "right", "insideH", "insideV"):
            borders.append(border(tag))
        insert_ordered(tbl_pr, borders, TBL_AFTER_BORDERS)
        for old in tbl_pr.findall(qn("w:tblLayout")):
            tbl_pr.remove(old)
        layout = OxmlElement("w:tblLayout")
        layout.set(qn("w:type"), "autofit")
        insert_ordered(tbl_pr, layout, TBL_AFTER_LAYOUT)
        for r_index, row in enumerate(table.rows):
            for cell in row.cells:
                for paragraph in cell.paragraphs:
                    paragraph.style = doc.styles["Compact"]
                    for run in paragraph.runs:
                        if r_index == 0:
                            run.font.bold = True
                if r_index == 0:
                    shading = OxmlElement("w:shd")
                    shading.set(qn("w:val"), "clear")
                    shading.set(qn("w:color"), "auto")
                    shading.set(qn("w:fill"), "E4EFFF")
                    cell._tc.get_or_add_tcPr().append(shading)
    # Title page on its own: page break after the date line; the TOC then starts a new page.
    for paragraph in doc.paragraphs:
        if paragraph.style.name == "Date":
            paragraph.add_run().add_break(WD_BREAK.PAGE)
            break
    # Ask Word to refresh the table of contents and page numbers when the file is opened.
    # CT_Settings is an ordered sequence: updateFields must precede footnotePr, compat, rsids, etc.
    settings = doc.settings.element
    update = OxmlElement("w:updateFields")
    update.set(qn("w:val"), "true")
    later = {"hdrShapeDefaults", "footnotePr", "endnotePr", "compat", "docVars", "rsids", "mathPr",
             "attachedSchema", "themeFontLang", "clrSchemeMapping", "doNotIncludeSubdocsInStats",
             "doNotAutoCompressPictures", "forceUpgrade", "captions", "readModeInkLockDown", "smartTagType",
             "schemaLibrary", "shapeDefaults", "doNotEmbedSmartTags", "decimalSymbol", "listSeparator"}
    anchor = next((child for child in settings if child.tag.split("}")[-1] in later), None)
    if anchor is not None:
        anchor.addprevious(update)
    else:
        settings.append(update)
    doc.save(path)


def main() -> None:
    OUT.mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        reference = Path(tmp) / "reference.docx"
        build_reference(reference)
        sources = sorted(p for p in SRC.glob("0*.md"))
        for md in sources:
            staged = Path(tmp) / md.name
            staged.write_text(preprocess(md.read_text(encoding="utf-8"), SRC), encoding="utf-8")
            target = OUT / (md.stem + ".docx")
            subprocess.run([
                PANDOC, str(staged), "-o", str(target), "--from", "markdown+pipe_tables+yaml_metadata_block",
                "--reference-doc", str(reference), "--resource-path", str(SRC),
                "--syntax-highlighting=tango",
            ], check=True, cwd=SRC)
            postprocess(target)
            print(f"built {target.relative_to(ROOT)}")


if __name__ == "__main__":
    sys.exit(main())
