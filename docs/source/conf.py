project = "Deephys"
copyright = ""
author = "MIT-Fujitsu Team"
release = "0.14.0"
extensions = [
    "sphinx.ext.duration",
    "sphinx.ext.doctest",
    "sphinx.ext.autodoc",
    "sphinx.ext.autosummary",
    "sphinx.ext.intersphinx",
    "sphinx_autodoc_typehints",
    "sphinx_tabs.tabs",
]
intersphinx_mapping = {
    "python": ("https://docs.python.org/3/", None),
    "torch": ("https://pytorch.org/docs/master", None),
}
intersphinx_disabled_domains = ["std"]
templates_path = ["_templates"]
html_theme = "sphinx_rtd_theme"
epub_show_urls = "footnote"
html_static_path = ["html"]
html_css_files = ["css/custom.css"]
