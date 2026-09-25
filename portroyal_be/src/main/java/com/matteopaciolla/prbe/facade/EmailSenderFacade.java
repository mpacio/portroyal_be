package com.matteopaciolla.prbe.facade;

import com.matteopaciolla.prbe.model.entity.EmailQueueEntity;
import com.matteopaciolla.prbe.repository.EmailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailSenderFacade {

    private final EmailRepository emailRepository;

    public void sendEmail(String recipientEmail, String recipientName, String subject, String htmlBody, String textBody) {
        EmailQueueEntity email = new EmailQueueEntity();
        email.setRecipientEmail(recipientEmail);
        email.setRecipientName(recipientName);
        email.setSubject(subject);
        email.setHtmlBody(htmlBody);
        email.setTextBody(textBody);
        emailRepository.save(email);
    }
}
