-- Patch from v1572 -> v1583
-- Change: FX-581: "Screenview" preview size
-- Author: Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)

ALTER TABLE FX_BINARY ADD COLUMN PREV4 LONGBLOB COMMENT 'if image: scaled to fit a 1024x768 box, else null';
ALTER TABLE FX_BINARY ADD COLUMN PREV4_WIDTH INTEGER UNSIGNED;
ALTER TABLE FX_BINARY ADD COLUMN PREV4_HEIGHT INTEGER UNSIGNED;
ALTER TABLE FX_BINARY ADD COLUMN PREV4SIZE INTEGER UNSIGNED;
ALTER TABLE FXS_BINARY_TRANSIT ADD COLUMN PREV4 LONGBLOB COMMENT 'if image: scaled to fit a 1024x768 box, else null';
ALTER TABLE FXS_BINARY_TRANSIT ADD COLUMN PREV4_WIDTH INTEGER UNSIGNED;
ALTER TABLE FXS_BINARY_TRANSIT ADD COLUMN PREV4_HEIGHT INTEGER UNSIGNED;
ALTER TABLE FXS_BINARY_TRANSIT ADD COLUMN PREV4SIZE INTEGER UNSIGNED;
UPDATE FXS_SCRIPTS SET SDATA=STRINGDECODE('/***************************************************************\r\n *  This file is part of the [fleXive](R) framework.\r\n *\r\n *  Copyright (c) 1999-2009\r\n *  UCS - unique computing solutions gmbh (http://www.ucs.at)\r\n *  All rights reserved\r\n *\r\n *  The [fleXive](R) project is free software; you can redistribute\r\n *  it and/or modify it under the terms of the GNU Lesser General Public\r\n *  License as published by the Free Software Foundation.\r\n *\r\n *  The GNU Lesser General Public License can be found at\r\n *  http://www.gnu.org/licenses/lgpl.html.\r\n *  A copy is found in the textfile LGPL.txt and important notices to the\r\n *  license from the author are found in LICENSE.txt distributed with\r\n *  these libraries.\r\n *\r\n *  This library is distributed in the hope that it will be useful,\r\n *  but WITHOUT ANY WARRANTY; without even the implied warranty of\r\n *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\r\n *  GNU General Public License for more details.\r\n *\r\n *  For further information about UCS - unique computing solutions gmbh,\r\n *  please see the company website: http://www.ucs.at\r\n *\r\n *  For further information about [fleXive](R), please see the\r\n *  project website: http://www.flexive.org\r\n *\r\n *\r\n *  This copyright notice MUST APPEAR in all copies of the file!\r\n ***************************************************************/\r\n\r\nimport com.flexive.shared.media.FxMediaEngine\r\nimport com.flexive.shared.media.FxMetadata\r\nimport com.flexive.shared.media.impl.FxMediaNativeEngine\r\nimport com.flexive.shared.value.BinaryDescriptor\r\n\r\n/*\r\nParameters:\r\n===========\r\n\"boolean processed\",\r\n\"boolean useDefaultPreview\",\r\n\"int defaultId\",\r\n\"String mimeType\",\r\n\"String metaData\",\r\n\"File binaryFile\",\r\n\"File previewFile1\",\r\n\"File previewFile2\",\r\n\"File previewFile3\",\r\n\"File previewFile4\",\r\n\"int[] dimensionPreview1\",\r\n\"int[] dimensionPreview2\",\r\n\"int[] dimensionPreview3\",\r\n\"int[] dimensionPreview4\"\r\n*/\r\nif (processed)\r\n    return\r\n\r\nboolean isImage = mimeType.startsWith(\"image/\")\r\nif (!isImage)\r\n    return\r\n\r\n//parse metadata\r\nFxMetadata meta = FxMediaEngine.identify(mimeType, binaryFile)\r\nmetaData = meta.toXML()\r\n//create previews\r\nString ext = \".JPG\"\r\nif (mimeType.endsWith(\"/png\"))\r\n    ext = \".PNG\"\r\nelse if (mimeType.endsWith(\"/gif\"))\r\n    ext = \".GIF\"\r\npreviewFile1 = File.createTempFile(\"PREV1\", ext)\r\npreviewFile2 = File.createTempFile(\"PREV2\", ext)\r\npreviewFile3 = File.createTempFile(\"PREV3\", ext)\r\npreviewFile4 = File.createTempFile(\"PREV4\", ext)\r\nif( meta.asImageMetadata().getWidth() <= 256 && meta.asImageMetadata().getHeight() <= 256 ) {\r\n    dimensionsPreview1 = FxMediaNativeEngine.scale(binaryFile, previewFile1, ext, BinaryDescriptor.PREVIEW1_BOX, BinaryDescriptor.PREVIEW1_BOX)\r\n    dimensionsPreview2 = FxMediaNativeEngine.scale(binaryFile, previewFile2, ext, BinaryDescriptor.PREVIEW2_BOX, BinaryDescriptor.PREVIEW2_BOX)\r\n    dimensionsPreview3 = FxMediaNativeEngine.scale(binaryFile, previewFile3, ext, BinaryDescriptor.PREVIEW3_BOX, BinaryDescriptor.PREVIEW3_BOX)\r\n    dimensionsPreview4 = FxMediaNativeEngine.scale(binaryFile, previewFile4, ext, BinaryDescriptor.SCREENVIEW_WIDTH, BinaryDescriptor.SCREENVIEW_HEIGHT)\r\n} else {\r\n    dimensionsPreview1 = FxMediaEngine.scale(binaryFile, previewFile1, ext, BinaryDescriptor.PREVIEW1_BOX, BinaryDescriptor.PREVIEW1_BOX)\r\n    dimensionsPreview2 = FxMediaEngine.scale(binaryFile, previewFile2, ext, BinaryDescriptor.PREVIEW2_BOX, BinaryDescriptor.PREVIEW2_BOX)\r\n    dimensionsPreview3 = FxMediaEngine.scale(binaryFile, previewFile3, ext, BinaryDescriptor.PREVIEW3_BOX, BinaryDescriptor.PREVIEW3_BOX)\r\n    dimensionsPreview4 = FxMediaEngine.scale(binaryFile, previewFile4, ext, BinaryDescriptor.SCREENVIEW_WIDTH, BinaryDescriptor.SCREENVIEW_HEIGHT)\r\n}\r\nprocessed = true\r\nuseDefaultPreview = false\r\n//println \"Processed ok!!\"') WHERE SNAME='BinaryProcessor_Images.gy';
UPDATE FXS_SCRIPTS SET SDATA=STRINGDECODE('/***************************************************************\r\n *  This file is part of the [fleXive](R) framework.\r\n *\r\n *  Copyright (c) 1999-2008\r\n *  UCS - unique computing solutions gmbh (http://www.ucs.at)\r\n *  All rights reserved\r\n *\r\n *  The [fleXive](R) project is free software; you can redistribute\r\n *  it and/or modify it under the terms of the GNU Lesser General Public\r\n *  License as published by the Free Software Foundation.\r\n *\r\n *  The GNU Lesser General Public License can be found at\r\n *  http://www.gnu.org/licenses/lgpl.html.\r\n *  A copy is found in the textfile LGPL.txt and important notices to the\r\n *  license from the author are found in LICENSE.txt distributed with\r\n *  these libraries.\r\n *\r\n *  This library is distributed in the hope that it will be useful,\r\n *  but WITHOUT ANY WARRANTY; without even the implied warranty of\r\n *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\r\n *  GNU General Public License for more details.\r\n *\r\n *  For further information about UCS - unique computing solutions gmbh,\r\n *  please see the company website: http://www.ucs.at\r\n *\r\n *  For further information about [fleXive](R), please see the\r\n *  project website: http://www.flexive.org\r\n *\r\n *\r\n *  This copyright notice MUST APPEAR in all copies of the file!\r\n ***************************************************************/\r\nimport com.flexive.extractor.ExtractedData\r\nimport com.flexive.extractor.Extractor\r\nimport com.flexive.shared.value.BinaryDescriptor\r\nimport com.flexive.shared.media.FxMediaEngine\r\nimport java.nio.channels.FileChannel\r\nimport java.nio.ByteBuffer\r\nimport com.sun.pdfview.*\r\nimport java.awt.image.BufferedImage\r\nimport java.awt.*\r\nimport javax.imageio.ImageIO\r\n\r\nif (processed) {\r\n    println \"already processed in documents ...\"\r\n    return\r\n}\r\n\r\nboolean isDocument = true\r\nint doc_previewId = BinaryDescriptor.SYS_UNKNOWN\r\n//Extractor.DOC_TYPE docType = Extractor.DOC_TYPE.WORD\r\nswitch (mimeType) {\r\n    case \"application/msword\":\r\n        doc_previewId = BinaryDescriptor.SYS_DOC\r\n        docType = Extractor.DocumentType.Word\r\n        break\r\n    case \"application/mspowerpoint\":\r\n        doc_previewId = BinaryDescriptor.SYS_PPT\r\n        docType = Extractor.DocumentType.Powerpoint\r\n        break\r\n    case \"application/msexcel\":\r\n        doc_previewId = BinaryDescriptor.SYS_XLS\r\n        docType = Extractor.DocumentType.Excel\r\n        break\r\n    case \"application/pdf\":\r\n        doc_previewId = BinaryDescriptor.SYS_PDF\r\n        docType = Extractor.DocumentType.PDF\r\n        break\r\n    case \"text/html\":\r\n        doc_previewId = BinaryDescriptor.SYS_HTML\r\n        docType = Extractor.DocumentType.HTML\r\n        break\r\n    default:\r\n        isDocument = false\r\n}\r\n\r\nif (!isDocument) {\r\n    println \"no document!\"\r\n    return\r\n}\r\nprintln \"processing a document!\"\r\ntry {\r\n    metaData = Extractor.extractData(binaryFile, docType).toXML()\r\n} catch (Throwable t) {\r\n    println \"Failed to extract data, returning empty set: \" + t.message\r\n    metaData = ExtractedData.toEmptyXML()\r\n}\r\n\r\nprocessed = true\r\nuseDefaultPreview = true\r\ndefaultId = doc_previewId\r\n\r\nif( docType == Extractor.DocumentType.PDF ) {\r\n    //try to render the first PDF page as image\r\n    try {\r\n        String ext = \".PNG\"\r\n        File imgFile = File.createTempFile(\"PDFIMG\", ext)\r\n\r\n        // set up the PDF reading\r\n        RandomAccessFile raf = new RandomAccessFile((File)binaryFile, \"r\");\r\n        FileChannel channel = raf.getChannel();\r\n        ByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());\r\n        PDFFile pdffile = new PDFFile(buf);\r\n\r\n        // get the first page\r\n        PDFPage page = pdffile.getPage(0);\r\n\r\n\r\n        // create and configure a graphics object\r\n        BufferedImage img = new BufferedImage((int)page.getBBox().getWidth(), (int)page.getBBox().getHeight(), BufferedImage.TYPE_INT_ARGB);\r\n        Graphics2D g2 = img.createGraphics();\r\n        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);\r\n\r\n        // do the actual drawing\r\n        PDFRenderer renderer = new PDFRenderer(page, g2,\r\n                new Rectangle(0, 0, (int)page.getBBox().getWidth(), (int)page.getBBox().getHeight()), null, Color.WHITE);\r\n        page.waitForFinish();\r\n        renderer.run();\r\n        ImageIO.write(img, ext.substring(1), imgFile)\r\n\r\n        previewFile1 = File.createTempFile(\"PREV1\", ext)\r\n        previewFile2 = File.createTempFile(\"PREV2\", ext)\r\n        previewFile3 = File.createTempFile(\"PREV3\", ext)\r\n        previewFile4 = File.createTempFile(\"PREV4\", ext)\r\n        dimensionsPreview1 = FxMediaEngine.scale(imgFile, previewFile1, ext, BinaryDescriptor.PREVIEW1_BOX, BinaryDescriptor.PREVIEW1_BOX)\r\n        dimensionsPreview2 = FxMediaEngine.scale(imgFile, previewFile2, ext, BinaryDescriptor.PREVIEW2_BOX, BinaryDescriptor.PREVIEW2_BOX)\r\n        dimensionsPreview3 = FxMediaEngine.scale(imgFile, previewFile3, ext, BinaryDescriptor.PREVIEW3_BOX, BinaryDescriptor.PREVIEW3_BOX)\r\n        dimensionsPreview4 = FxMediaEngine.scale(imgFile, previewFile4, ext, BinaryDescriptor.SCREENVIEW_WIDTH, BinaryDescriptor.SCREENVIEW_HEIGHT)\r\n        if( !imgFile.delete() )\r\n            imgFile.deleteOnExit()\r\n        useDefaultPreview = false\r\n    } catch (Exception e) {\r\n        println \"PDFRenderer failed: \"+e.message+\" - using default preview\"\r\n    }\r\n}\r\n\r\nprintln \"Processed document ok\"') WHERE SNAME='BinaryProcessor_Documents.gy';