/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2014
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU Lesser General Public
 *  License version 2.1 or higher as published by the Free Software Foundation.
 *
 *  The GNU Lesser General Public License can be found at
 *  http://www.gnu.org/licenses/lgpl.html.
 *  A copy is found in the textfile LGPL.txt and important notices to the
 *  license from the author are found in LICENSE.txt distributed with
 *  these libraries.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  For further information about UCS - unique computing solutions gmbh,
 *  please see the company website: http://www.ucs.at
 *
 *  For further information about [fleXive](R), please see the
 *  project website: http://www.flexive.org
 *
 *
 *  This copyright notice MUST APPEAR in all copies of the file!
 ***************************************************************/
package com.flexive.shared;

import com.flexive.shared.exceptions.FxApplicationException;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.net.QuotedPrintableCodec;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.util.ByteArrayDataSource;
import java.io.*;

/**
 * Email Utilities.
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxMailUtils {

    private static final String BOUNDARY1 = "FxMuLtIpArT_BoUnDaRy";
    private static final String BOUNDARY2 = "FxMuLtIpArT2BoUnDaRy";

    /**
     * Simple check for valid email address
     *
     * @param email address to check
     * @return valid
     */
    public static boolean isValidEmail(String email) {
        if (StringUtils.isEmpty(email))
            return false;
        if (email.indexOf('.') <= 0 || email.indexOf('@') <= 0)
            return false;
        return true;
    }

    /**
     * Encode an input stream as email attachment
     *
     * @param mimeType mime type
     * @param fileName filename
     * @param input     the data to encode
     * @return file encoded as email attachment
     * @throws IOException on errors
     */
    public static String encodeAttachment(String mimeType, String fileName, InputStream input) throws IOException {
        final byte[] data = IOUtils.toByteArray(input);
        final StringBuilder encoded = new StringBuilder(data.length * 4);
        encoded.append("Content-Type: ").append(mimeType).append(";\n");
        encoded.append(" name=\"").append(fileName).append("\"\n");
        encoded.append("Content-Transfer-Encoding: base64\n");
        encoded.append("Content-Disposition: attachment;\n");
        encoded.append(" filename=\"").append(fileName).append("\"\n\n");
        int k;
        final String attachmentEncoded = new String(Base64.encodeBase64(data), "UTF-8");
        int kLines = attachmentEncoded.length() / 74;
        for (k = 0; k < kLines; k++) {
            encoded.append(attachmentEncoded.substring(k * 74, k * 74 + 74)).append("\n");
        }
        if (kLines * 74 < attachmentEncoded.length())
            encoded.append(attachmentEncoded.substring(k * 74, attachmentEncoded.length())).append("\n");
        return encoded.toString();
    }

    /**
     * Encode a file as email attachment
     *
     * @param mimeType mime type
     * @param fileName filename
     * @param data     the file to encode
     * @return file encoded as email attachment
     * @throws IOException on errors
     */
    public static String encodeAttachment(String mimeType, String fileName, File data) throws IOException {
        return encodeAttachment(mimeType, fileName, new FileInputStream(data));
    }

    /**
     * Sends an email
     *
     * @param SMTPServer      IP Address of the SMTP server
     * @param subject         subject of the email
     * @param textBody        plain text
     * @param htmlBody        html text
     * @param to              recipient
     * @param cc              cc recepient
     * @param bcc             bcc recipient
     * @param from            sender
     * @param replyTo         reply-to address
     * @param mimeAttachments strings containing mime encoded attachments
     * @throws FxApplicationException on errors
     */
    public static void sendMail(String SMTPServer, String subject, String textBody, String htmlBody, String to,
                                String cc, String bcc, String from, String replyTo, String... mimeAttachments) throws FxApplicationException {

        try {
            // Set the mail server
            java.util.Properties properties = System.getProperties();
            if (SMTPServer != null) properties.put("mail.smtp.host", SMTPServer);

            // Get a session and create a new message
            javax.mail.Session session = javax.mail.Session.getInstance(properties, null);
            MimeMessage msg = new MimeMessage(session);

            // Set the sender
            if (StringUtils.isBlank(from))
                msg.setFrom(); // Uses local IP Adress and the user under which the server is running
            else {
                msg.setFrom(createAddress(from));
            }

            if (!StringUtils.isBlank(replyTo))
                msg.setReplyTo(encodePersonal(InternetAddress.parse(replyTo, false)));


            // Set the To, Cc and Bcc fields
            if (!StringUtils.isBlank(to))
                msg.setRecipients(MimeMessage.RecipientType.TO, encodePersonal(InternetAddress.parse(to, false)));
            if (!StringUtils.isBlank(cc))
                msg.setRecipients(MimeMessage.RecipientType.CC, encodePersonal(InternetAddress.parse(cc, false)));
            if (!StringUtils.isBlank(bcc))
                msg.setRecipients(MimeMessage.RecipientType.BCC, encodePersonal(InternetAddress.parse(bcc, false)));

            // Set the subject
            msg.setSubject(subject, "UTF-8");

            String sMainType = "Multipart/Alternative;\n\tboundary=\"" + BOUNDARY1 + "\"";

            StringBuilder body = new StringBuilder(5000);

            if (mimeAttachments.length > 0) {
                sMainType = "Multipart/Mixed;\n\tboundary=\"" + BOUNDARY2 + "\"";
                body.append("This is a multi-part message in MIME format.\n\n");
                body.append("--" + BOUNDARY2 + "\n");
                body.append("Content-Type: Multipart/Alternative;\n\tboundary=\"" + BOUNDARY1 + "\"\n\n\n");
            }

            if (textBody.length() > 0) {
                body.append("--" + BOUNDARY1 + "\n");
                body.append("Content-Type: text/plain; charset=\"UTF-8\"\n");
                body.append("Content-Transfer-Encoding: quoted-printable\n\n");
                body.append(encodeQuotedPrintable(textBody)).append("\n");
            }
            if (htmlBody.length() > 0) {
                body.append("--" + BOUNDARY1 + "\n");
                body.append("Content-Type: text/html;\n\tcharset=\"UTF-8\"\n");
                body.append("Content-Transfer-Encoding: quoted-printable\n\n");
                if (htmlBody.toLowerCase().indexOf("<html>") < 0) {
                    body.append("<HTML><HEAD>\n<TITLE></TITLE>\n");
                    body.append("<META http-equiv=3DContent-Type content=3D\"text/html; charset=3Dutf-8\"></HEAD>\n<BODY>\n");
                    body.append(encodeQuotedPrintable(htmlBody)).append("</BODY></HTML>\n");
                } else
                    body.append(encodeQuotedPrintable(htmlBody)).append("\n");
            }

            body.append("\n--" + BOUNDARY1 + "--\n");

            if (mimeAttachments.length > 0) {
                for (String mimeAttachment : mimeAttachments) {
                    body.append("\n--" + BOUNDARY2 + "\n");
                    body.append(mimeAttachment).append("\n");
                }
                body.append("\n--" + BOUNDARY2 + "--\n");
            }

            msg.setDataHandler(new javax.activation.DataHandler(new ByteArrayDataSource(body.toString(), sMainType)));

            // Set the header
            msg.setHeader("X-Mailer", "JavaMailer");

            // Set the sent date
            msg.setSentDate(new java.util.Date());

            // Send the message
            javax.mail.Transport.send(msg);
        } catch (AddressException e) {
            throw new FxApplicationException(e, "ex.messaging.mail.address", e.getRef());
        } catch (javax.mail.MessagingException e) {
            throw new FxApplicationException(e, "ex.messaging.mail.send", e.getMessage());
        } catch (IOException e) {
            throw new FxApplicationException(e, "ex.messaging.mail.send", e.getMessage());
        } catch (EncoderException e) {
            throw new FxApplicationException(e, "ex.messaging.mail.send", e.getMessage());
        }
    }

    private static InternetAddress[] encodePersonal(InternetAddress[] addresses) throws UnsupportedEncodingException {
        if (addresses == null) {
            return null;
        }
        for (InternetAddress address : addresses) {
            if (address.getPersonal() != null) {
                address.setPersonal(address.getPersonal(), "UTF-8");
            }
        }
        return addresses;
    }

    private static InternetAddress createAddress(String from) throws AddressException, UnsupportedEncodingException {
        final InternetAddress address = new InternetAddress(from);
        if (address.getPersonal() != null) {
            // encode name which is more likely to use non-ASCII characters
            address.setPersonal(address.getPersonal(), "UTF-8");
        }
        return address;
    }

    private static String encodeQuotedPrintable(String textBody) throws EncoderException {
        return new QuotedPrintableCodec("UTF-8").encode(textBody);
    }
}
