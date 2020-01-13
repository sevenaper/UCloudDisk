package com.netdiskteam.netdisk.ticket;

import java.util.HashMap;
import java.util.UUID;

public class UserTickets {
    private static final HashMap<Integer, String> tickets = new HashMap<>();

    public static String getTicket(Integer userID) {
        synchronized(tickets) {
            return tickets.get(userID);
        }
    }

    public static synchronized String addTicket(int userID) {
        synchronized(tickets) {
            tickets.remove(userID);
            // Generate a new ticket
            String ticket = UUID.randomUUID().toString();
            tickets.put(userID, ticket);
            return ticket;
        }
    }

    public static synchronized void removeTicket(int userID, String ticket) {
        synchronized(tickets) {
            tickets.remove(userID, ticket);
        }
    }
}
