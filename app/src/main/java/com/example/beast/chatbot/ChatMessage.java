package com.example.beast.chatbot;


/**
 * Created by beast on 14/4/17.
 */

public class ChatMessage {

    String msgText;
    String msgUser;


    public ChatMessage(String msgText, String msgUser){
        this.msgText = msgText;
        this.msgUser = msgUser;

    }
    public ChatMessage() {

    }

    public void setMsgText(String msgText) {
        this.msgText = msgText;
    }

    public String getMsgText(){
        return msgText;
    }


    public void setMsgUser(String msgUser) {
        this.msgUser = msgUser;
    }

    public String getMsgUser() {
        return msgUser;
    }

}
