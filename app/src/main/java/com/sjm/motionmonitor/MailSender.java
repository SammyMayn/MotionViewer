package com.sjm.motionmonitor;

import android.content.Context;
import android.os.AsyncTask;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

//import android.se.omapi.Session;

public abstract class MailSender extends AsyncTask<Void,Void,Void> {
//public class MailSender extends javax.mail.Authenticator {
    //Declaring Variables
    private Context context;
    private Session session;

    //Information to send email
    //private String mailhost = "smtp.gmail.com";
    private String email;
    private String subject;
    private String message;
    private String PW;
    private String attach;
    private int numAttach;
    private List<String> listFilesToSend;
    //public MailSender(Context context, String email, String subject, String message,String PWord){
    public MailSender(String email, String subject, String message, String PWord, List<String> listFiles, int NumAttachments){
        //Initializing variables
        //this.context = context;
        this.email = email;
        this.subject = subject;
        this.message = message;
        this.PW = PWord;
        this.attach = listFiles.get(0);
        this.listFilesToSend = listFiles;
        this.numAttach = NumAttachments;
    }
    @Override
    protected Void doInBackground(Void... params) {
        //Creating properties
        Properties props = new Properties();

        //Configuring properties for gmail
        //If you are not using gmail you may need to change the values
        //props.put("mail.smtp.host", "smtp.mail.yahoo.com");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");

        //Creating a new session
        session = Session.getDefaultInstance(props,
                new javax.mail.Authenticator() {
                    //Authenticating the password
                    protected PasswordAuthentication getPasswordAuthentication() {

                        return new PasswordAuthentication(email, PW);
                    }
                });

        try {
            if (numAttach == 999)
                return null;
            //Creating MimeMessage object
            MimeMessage mm = new MimeMessage(session);
            // define body part for the attachment
            MimeBodyPart attachPart ;//= new MimeBodyPart();
            Multipart multipart = new MimeMultipart();

            //Setting sender address
            mm.setFrom(new InternetAddress(email));
            //Adding receiver
            mm.addRecipient(Message.RecipientType.TO, new InternetAddress(email));
            //Adding subject
            mm.setSubject(subject);
            //Adding message
            mm.setText(message);

            try {
                for (int i = 0 ;i < numAttach;i++) {
                    attachFile(listFilesToSend.get(i), multipart);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            mm.setContent(multipart);

            //Sending email
            Transport.send(mm);

        } catch (MessagingException e) {
            e.printStackTrace();
        }
        return null;
    }
    public void attachFile(String file, Multipart multipart) {
        MimeBodyPart attachPart ;
        try {
            attachPart = new MimeBodyPart();
            attachPart.attachFile(file);
            multipart.addBodyPart(attachPart);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        }


    }
}
