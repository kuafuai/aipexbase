package com.kuafuai.common.mail.client;

import com.kuafuai.common.mail.spec.MailAttachment;
import com.kuafuai.common.mail.spec.MailMessage;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeUtility;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class MailReceiveClient {

    public List<MailMessage> receive(String host, int port, String user, String password,
                                     String folderName, int count, long lastUid) {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", host);
        props.put("mail.imaps.port", String.valueOf(port));
        props.put("mail.imaps.ssl.enable", "true");

        Session session = Session.getInstance(props);
        Store store = null;
        Folder folder = null;
        List<MailMessage> result = new ArrayList<>();
        try {
            store = session.getStore("imaps");
            store.connect(host, port, user, password);
            folder = store.getFolder(folderName);
            folder.open(Folder.READ_ONLY);

            UIDFolder uidFolder = (UIDFolder) folder;
            Message[] messages;
            if (lastUid > 0) {
                messages = uidFolder.getMessagesByUID(lastUid + 1, UIDFolder.LASTUID);
            } else {
                int total = folder.getMessageCount();
                if (total == 0) {
                    return result;
                }
                int start = Math.max(1, total - count + 1);
                messages = folder.getMessages(start, total);
            }

            for (Message message : messages) {
                if (message == null) continue;
                result.add(parseMessage(message, uidFolder));
            }
        } catch (MessagingException | IOException e) {
            throw new RuntimeException(e.getMessage());
        } finally {
            if (folder != null && folder.isOpen()) {
                try {
                    folder.close(false);
                } catch (MessagingException ignored) {
                }
            }
            if (store != null && store.isConnected()) {
                try {
                    store.close();
                } catch (MessagingException ignored) {
                }
            }
        }
        return result;
    }

    private MailMessage parseMessage(Message message, UIDFolder uidFolder) throws MessagingException, IOException {
        long uid = uidFolder.getUID(message);

        String[] idHeaders = message.getHeader("Message-ID");
        String messageId = (idHeaders != null && idHeaders.length > 0) ? idHeaders[0] : "";

        String from = extractFirstAddress(message.getFrom());
        List<String> to = extractAddresses(message.getRecipients(Message.RecipientType.TO));
        List<String> cc = extractAddresses(message.getRecipients(Message.RecipientType.CC));

        String subject = message.getSubject() != null
                ? MimeUtility.decodeText(message.getSubject()) : "";

        ParsedContent content = new ParsedContent();
        parseMimePart(message, content);

        if (content.plainText.isEmpty() && !content.htmlBody.isEmpty()) {
            content.plainText = content.htmlBody.replaceAll("<[^>]+>", "").trim();
        } else if (content.htmlBody.isEmpty() && !content.plainText.isEmpty()) {
            content.htmlBody = "<pre>" + content.plainText + "</pre>";
        }

        return MailMessage.builder()
                .uid(uid)
                .messageId(messageId)
                .from(from)
                .to(to)
                .cc(cc)
                .subject(subject)
                .sentDate(message.getSentDate())
                .body(content.plainText)
                .attachments(content.attachments)
                .build();
    }

    private void parseMimePart(Part part, ParsedContent result) throws MessagingException, IOException {
        String disposition = part.getDisposition();
        if (Part.ATTACHMENT.equalsIgnoreCase(disposition)) {
            result.attachments.add(buildAttachment(part));
            return;
        }
        if (part.isMimeType("text/plain") && result.plainText.isEmpty()) {
            result.plainText = (String) part.getContent();
        } else if (part.isMimeType("text/html") && result.htmlBody.isEmpty()) {
            result.htmlBody = (String) part.getContent();
        } else if (part.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) part.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                parseMimePart(mp.getBodyPart(i), result);
            }
        } else if (part.isMimeType("message/rfc822")) {
            parseMimePart((Part) part.getContent(), result);
        }
    }

    private MailAttachment buildAttachment(Part part) throws MessagingException {
        String name = part.getFileName();
        if (name != null) {
            try {
                name = MimeUtility.decodeText(name);
            } catch (Exception ignored) {
            }
        }
        return MailAttachment.builder()
                .name(name != null ? name : "")
                .contentType(part.getContentType())
                .size(part.getSize())
                .build();
    }

    private String extractFirstAddress(Address[] addresses) {
        if (addresses == null || addresses.length == 0) return "";
        return addresses[0] instanceof InternetAddress
                ? ((InternetAddress) addresses[0]).toUnicodeString()
                : addresses[0].toString();
    }

    private List<String> extractAddresses(Address[] addresses) {
        if (addresses == null) return Collections.emptyList();
        List<String> result = new ArrayList<>(addresses.length);
        for (Address addr : addresses) {
            result.add(addr instanceof InternetAddress
                    ? ((InternetAddress) addr).toUnicodeString()
                    : addr.toString());
        }
        return result;
    }

    private static class ParsedContent {
        String plainText = "";
        String htmlBody = "";
        List<MailAttachment> attachments = new ArrayList<>();
    }
}
