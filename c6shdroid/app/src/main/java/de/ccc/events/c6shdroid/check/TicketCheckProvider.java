package de.ccc.events.c6shdroid.check;


import org.json.JSONObject;

import java.util.List;

public interface TicketCheckProvider {

    class CheckResult {
        public enum Type {
            VALID, ERROR, CONFIRMATION, INPUT
        }

        private Type type;
        private String message;
        private String missing_field;

        public CheckResult(Type type, String message) {
            this.type = type;
            this.message = message;
        }

        public CheckResult(Type type) {
            this.type = type;
        }

        public Type getType() {
            return type;
        }

        public void setType(Type type) {
            this.type = type;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getMissingField() {
            return missing_field;
        }

        public void setMissingField(String missing_field) {
            this.missing_field = missing_field;
        }
    }

    class SearchResult {

        private String secret;
        private String ticket;
        private String order_code;
        private boolean paid;
        private boolean redeemed;

        public SearchResult() { }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public boolean isPaid() {
            return paid;
        }

        public void setPaid(boolean paid) {
            this.paid = paid;
        }

        public boolean isRedeemed() {
            return redeemed;
        }

        public void setRedeemed(boolean redeemed) {
            this.redeemed = redeemed;
        }

        public String getTicket() {
            return ticket;
        }

        public void setTicket(String ticket) {
            this.ticket = ticket;
        }

        public String getOrderCode() {
            return order_code;
        }

        public void setOrderCode(String order_code) {
            this.order_code = order_code;
        }
    }

    CheckResult check(String ticketid, JSONObject options);
    List<SearchResult> search(String query) throws CheckException;
}
