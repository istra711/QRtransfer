# SEPA QR-Transfer Plugin for Jameica/Hibiscus

A Jameica plugin that reads SEPA payment QR codes and creates transfer drafts in the Hibiscus online banking application.

## About This Project - AI Feasibility Study

This plugin is the result of a **feasibility study** to test the capabilities of current AI systems. The entire development - from the initial concept to the final upload on GitHub - was carried out entirely by AI. **The human author did not write a single line of code.**

The project demonstrates that modern AI assistants can handle complex, multi-step software engineering tasks autonomously, including:
- Understanding domain-specific frameworks (Jameica/Hibiscus)
- Implementing multiple input methods (clipboard, file, PDF)
- Working with external libraries (ZXing, Apache PDFBox)
- Debugging and fixing integration issues
- Setting up build systems (Apache Ant)
- Managing Git repositories and publishing to GitHub

The result exceeded expectations and shows that AI can serve as a capable "co-developer" for real-world applications.

### Development Environment

- **AI Assistant:** [OpenCode](https://opencode.ai) using the `mimo/mimo-v2-free` model
- **Operating System:** Windows
- **Human Role:** Project owner and tester - provided requirements, tested the plugin in Jameica, reported bugs, and guided the development through natural language conversation
- **AI Role:** Full-stack developer - wrote all Java code, XML configuration, build scripts, managed dependencies, and handled Git/GitHub operations

## Features

- **Three input methods:**
  - **Clipboard** - Copy a QR code image and paste it directly
  - **Image file** - Open PNG, JPG, or BMP files containing a QR code
  - **PDF invoice** - Extract QR codes directly from PDF documents

- **Supported QR code formats:**
  - **EPC (BCD)** - The standard European Payment Council format used on invoices
  - **EMV (TLV)** - The EMV standard format used in payment terminals

- **Automatic data extraction:**
  - IBAN and BIC
  - Recipient name and city
  - Amount and currency (EUR)
  - Payment reference / purpose

- **Seamless Hibiscus integration:**
  - Adds a "QR-Code Ueberweisung" submenu under Zahlungsverkehr in the navigation tree
  - Creates a pre-filled transfer draft ready for review and sending
  - Supports both domestic (German) and international SEPA transfers

## Installation

1. Download the latest release from the [Releases](https://github.com/istra711/hbci.qrtransfer/releases) page
2. Open Jameica
3. Go to **Datei > Plugins online suchen... > Plugin manuell installieren...**
4. Select `hbci.qrtransfer-1.0.0.zip`
5. Restart Jameica

### Manual Build

If you prefer to build from source:

```bash
# Requirements
# - JDK 8 or higher
# - Apache Ant

# Set JAVA_HOME (adjust path as needed)
export JAVA_HOME="/path/to/jdk"

# Build
ant dist

# The plugin will be in dist/hbci.qrtransfer/
```

## Usage

1. In Hibiscus, navigate to **Zahlungsverkehr > QR-Code Ueberweisung**
2. Choose one of the three input methods:
   - **Aus Zwischenablage** - Reads QR code from clipboard
   - **Aus Datei** - Opens a file dialog for image files
   - **Aus PDF-Datei** - Opens a file dialog for PDF invoices
3. The plugin displays all extracted SEPA data
4. Click **Ueberweisung anlegen** to create a transfer draft in Hibiscus
5. Review the data and send the transfer

### Keyboard Shortcuts

| Action | Shortcut |
|--------|----------|
| QR from clipboard | `Ctrl+Shift+Q` |
| QR from file | `Ctrl+Shift+F` |

## Example EPC QR Code

```
BCD
001
1
SCT
BICXXXMH
Max Mustermann
DE89370400440532013000
EUR123.45
Rechnung Nr. 12345
```

## Technical Details

### Architecture

```
src/de/willuhn/jameica/hbci/qrtransfer/
├── QRTransferPlugin.java          # Plugin entry point
├── action/
│   ├── QRCodeAction.java          # Read QR from clipboard
│   ├── QRFileAction.java          # Read QR from image file
│   └── QRPdfAction.java           # Read QR from PDF
├── gui/
│   ├── QRCodeView.java            # Preview and create transfer
│   ├── WelcomeAction.java         # Navigation action
│   └── WelcomeView.java           # Landing page
├── model/
│   └── SepaData.java              # SEPA data model
└── parser/
    ├── QrCodeParser.java          # Parser interface
    ├── EpcParser.java             # EPC (BCD) format parser
    ├── EmvParser.java             # EMV (TLV) format parser
    └── ParserException.java       # Parser errors
```

### Dependencies

- **Jameica** 2.0+ - Plugin framework
- **Hibiscus** 2.0+ - Online banking plugin
- **ZXing** 3.5.3 - QR code decoding library
- **Apache PDFBox** 3.0.3 - PDF rendering (for PDF QR extraction)
- **SWT** - Standard Jameica GUI toolkit

## License

This project is licensed under the GPL License - see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please open an issue or pull request on GitHub.
