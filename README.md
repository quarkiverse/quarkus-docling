[![Version](https://img.shields.io/maven-central/v/io.quarkiverse.docling/quarkus-docling?logo=apache-maven&style=flat-square)](https://central.sonatype.com/artifact/io.quarkiverse.docling/quarkus-docling-parent)

<!-- ALL-CONTRIBUTORS-BADGE:START - Do not remove or modify this section -->
[![All Contributors](https://img.shields.io/badge/all_contributors-2-orange.svg?style=flat-square)](#contributors-)
<!-- ALL-CONTRIBUTORS-BADGE:END -->

# Quarkus Docling

This is a Quarkus extension for the [Docling project](https://github.com/docling-project). Docling simplifies document processing, parsing diverse formats — including advanced PDF understanding — and providing seamless integrations with the gen AI ecosystem.

<p align="center">
  <a href="https://github.com/docling-project/docling-serve">
    <img loading="lazy" alt="Docling" src="https://github.com/docling-project/docling-serve/raw/main/docs/assets/docling-serve-pic.png" width="30%"/>
  </a>
</p>

## Docling Features

* 🗂️ Parsing of [multiple document formats][supported_formats] incl. PDF, DOCX, XLSX, HTML, images, and more
* 📑 Advanced PDF understanding incl. page layout, reading order, table structure, code, formulas, image classification, and more
* 🧬 Unified, expressive [DoclingDocument][docling_document] representation format
* ↪️ Various [export formats][supported_formats] and options, including Markdown, HTML, and lossless JSON
* 🔒 Local execution capabilities for sensitive data and air-gapped environments
* 🤖 Plug-and-play [integrations][integrations] incl. LangChain, LlamaIndex, Crew AI & Haystack for agentic AI
* 🔍 Extensive OCR support for scanned PDFs and images
* 🥚 Support of several Visual Language Models ([SmolDocling](https://huggingface.co/ds4sd/SmolDocling-256M-preview))
* 💻 Simple and convenient CLI

## Quarkus Docling Features

Currently, this extension is a set of wrappers around the [Docling Serve](https://github.com/docling-project/docling-serve) project, which exposes Docling as a REST API. It also provides a Dev Service and Dev UI integrations.

The eventual goal is to unify the [DoclingDocument][docling document] format with [LangChain4j's `Document` abstraction](https://docs.langchain4j.dev/tutorials/rag#document) so that Docling can be used in a LangChain4j RAG pipeline for ingesting data.

## Contributors ✨

Thanks goes to these wonderful people ([emoji key](https://allcontributors.org/docs/en/emoji-key)):

<!-- ALL-CONTRIBUTORS-LIST:START - Do not remove or modify this section -->
<!-- prettier-ignore-start -->
<!-- markdownlint-disable -->
<table>
  <tbody>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="https://developers.redhat.com/author/eric-deandrea"><img src="https://avatars.githubusercontent.com/u/363447?v=4?s=100" width="100px;" alt="Eric Deandrea"/><br /><sub><b>Eric Deandrea</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-docling/commits?author=edeandrea" title="Code">💻</a> <a href="#maintenance-edeandrea" title="Maintenance">🚧</a> <a href="https://github.com/quarkiverse/quarkus-docling/commits?author=edeandrea" title="Tests">⚠️</a> <a href="#ideas-edeandrea" title="Ideas, Planning, & Feedback">🤔</a> <a href="#content-edeandrea" title="Content">🖋</a> <a href="https://github.com/quarkiverse/quarkus-docling/commits?author=edeandrea" title="Documentation">📖</a></td>
      <td align="center" valign="top" width="14.28%"><a href="http://www.lordofthejars.com"><img src="https://avatars.githubusercontent.com/u/1517153?v=4?s=100" width="100px;" alt="Alex Soto"/><br /><sub><b>Alex Soto</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-docling/commits?author=lordofthejars" title="Code">💻</a> <a href="#maintenance-lordofthejars" title="Maintenance">🚧</a> <a href="#content-lordofthejars" title="Content">🖋</a> <a href="https://github.com/quarkiverse/quarkus-docling/commits?author=lordofthejars" title="Documentation">📖</a> <a href="#ideas-lordofthejars" title="Ideas, Planning, & Feedback">🤔</a></td>
    </tr>
  </tbody>
</table>

<!-- markdownlint-restore -->
<!-- prettier-ignore-end -->

<!-- ALL-CONTRIBUTORS-LIST:END -->

This project follows the [all-contributors](https://github.com/all-contributors/all-contributors) specification. Contributions of any kind welcome!

[supported_formats]: https://docling-project.github.io/docling/usage/supported_formats/
[docling_document]: https://docling-project.github.io/docling/concepts/docling_document/
[integrations]: https://docling-project.github.io/docling/integrations/
