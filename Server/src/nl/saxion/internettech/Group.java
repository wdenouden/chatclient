package nl.saxion.internettech;

import java.util.ArrayList;

public class Group {

    private String groupname;
    private String ownername;
    private ArrayList<Server.ClientThread> users;

    /**
     * Constructor
     * @param groupname
     * @param owner
     */
    public Group(String groupname, Server.ClientThread owner) {
        this.groupname = groupname;
        users = new ArrayList<>();
        ownername = owner.getUsername();
        users.add(owner);
    }

    /*
     *
     * @returng groupname
     */
    public String getGroupname() {
        return groupname;
    }

    /**
     *
     * @return users
     */
    public ArrayList<Server.ClientThread> getUsers() {
        return users;
    }

    /**
     * Add user to group
     * @param ct
     * @return
     */
    public boolean joinGroup(Server.ClientThread ct) {
        if(!userExists(ct.getUsername())) {
            users.add(ct);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Remove user from group
     * @param ct
     * @return
     */
    public String leaveGroup(Server.ClientThread ct) {
        // Als owner de groep verlaat, wordt de eerst volgende persoon de owner
        // als er geen andere gebruikers zijn, wordt de groep verwijderd
        if(ct.getUsername().equals(ownername)) {
            if(users.size() > 1) {
                ownername = users.get(1).getUsername();
            } else {
                return "DEL";
            }
        }

        for(Server.ClientThread user: users) {
            if(ct.getUsername().equals((user.getUsername()))) {
                users.remove(ct);
                return "OK";
            }
        }
        return "ERR";
    }

    /**
     * Send message to all users in group
     * @param message
     */
    public void sendMessage(String message) {
        for(Server.ClientThread ct: users) {
            ct.writeToClient(message);
        }
    }

    /**
     * Kick user from this group
     * @param username
     * @param ownername
     * @return
     */
    public String kickUser(String username, String ownername) {
        if(!username.equals(ownername)) {
            if(this.ownername.equals(ownername)) {
                for(Server.ClientThread ct: users) {
                    if(ct.getUsername().equals(username)) {
                        users.remove(ct);
                        return "+OK user kicked from group";
                    }
                }
                return "-ERR user not in group";
            } else {
                return "-ERR only the owner can kick members";
            }
        } else {
            return "-ERR you can't kick yourself";
        }
    }

    /**
     * Show all users in group
     * @return
     */
    public String showUsers() {
        String message = "";
        for(Server.ClientThread ct: users) {
            if(ct.getUsername() != null) {
                message += "- " + ct.getUsername() + "\n";
            }
        }
        return message;
    }

    /**
     * check if user already exists in group
     * @param username
     * @return
     */
    private boolean userExists(String username) {
        for(Server.ClientThread user: users) {
            if(user.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }

}
