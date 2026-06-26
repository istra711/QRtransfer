# SEPA QR-Transfer Plugin for Jameica/Hibiscus

A Jameica plugin that reads SEPA payment QR codes and creates transfer drafts in the Hibiscus online banking application.

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
2. Extract the `hbci.qrtransfer` folder into your Jameica plugins directory:
   - **Windows:** `C:\Programme\Jameica\plugins\` or portable: `<Jameica-Verzeichnis>\plugins\`
   - **Linux:** `~/.jameica/plugins/`
   - **Mac:** `~/Library/Jameica/plugins/`
3. Restart Jameica

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
