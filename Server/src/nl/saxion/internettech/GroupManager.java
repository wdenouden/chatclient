package nl.saxion.internettech;

import java.util.ArrayList;

public class GroupManager {

    // List with groups
    private ArrayList<Group> groups;

    /**
     * Constructor
     */
    public GroupManager() {
        groups = new ArrayList<>();
    }

    /**
     * Add new group
     * @param group
     * @return
     */
    public String addGroup(Group group) {
        if(!groupExists(group.getGroupname())) {
            groups.add(group);
            return "+OK group added";
        } else {
            return "-ERR group already exists";
        }
    }

    /**
     * Join existing group
     * @param groupname
     * @param ct
     * @return
     */
    public String joinGroup(String groupname, Server.ClientThread ct) {
        if(groupExists(groupname)) {
            for(Group g: groups) {
                if(g.getGroupname().equals(groupname)) {
                    if(g.joinGroup(ct)) {
                        return "+OK group joined";
                    } else {
                        return "-ERR already in group";
                    }
                }
            }
            return "-ERR group doesn't exist";
        } else {
            return "-ERR group doesn't exist";
        }
    }

    /**
     * Leave existing group
     * @param groupname
     * @param ct
     * @return
     */
    public String leaveGroup(String groupname, Server.ClientThread ct) {
        if(groupExists(groupname)) {
            for(Group g: groups) {
                if(g.getGroupname().equals(groupname)) {
                    switch(g.leaveGroup(ct)) {
                        case "DEL":
                            if(deleteGroup(g)) {
                                return "+OK group deleted";
                            } else {
                                return "-ERR";
                            }
                        case "OK":
                            return "+OK group left";
                        case "ERR":
                            return "-ERR user not in group";
                    }
                }
            }
            return "-ERR group doesn't exist";
        } else {
            return "-ERR group doesn't exist";
        }
    }

    /**
     * Delete existing group
     * @param group
     * @return
     */
    public boolean deleteGroup(Group group) {
        for(Group g: groups) {
            if(g.getGroupname().equals(group.getGroupname())) {
                groups.remove(group);
                return true;
            }
        }
        return false;
    }

    /**
     * Send message to all users from group
     * @param splits
     * @param username
     * @return
     */
    public String sendGroupMessage(String[] splits, String username) {
        String groupname = splits[1];
        String msg = "GROUP (" + groupname + ") " + username + " says: ";
        for(int i = 2; i < splits.length; i++) {
            msg += splits[i] + " ";
        }

        for(Group g: groups) {
            if(g.getGroupname().equals(groupname)) {
                for(Server.ClientThread ct: g.getUsers()) {
                    if(ct.getUsername().equals(username)) {
                        g.sendMessage(msg);
                        return "+OK message sent to group";
                    }
                }
                return "-ERR not in group";
            }
        }
        return "-ERR group not found";
    }

    /**
     * Kick user from group
     * @param groupname
     * @param username
     * @param ownername
     * @return
     */
    public String kickUser(String groupname, String username, String ownername) {
        if(groupExists(groupname)) {
            for(Group g: groups) {
                if(g.getGroupname().equals(groupname)) {
                    return g.kickUser(username, ownername);
                }
            }
            return "-ERR group doesn't exist";
        } else {
            return "-ERR group doesn't exist";
        }
    }

    /**
     * Show all groups with their members
     * @return
     */
    public String showGroups() {
        String msg = "";
        for(Group g: groups) {
            msg += g.getGroupname() + "\n";
            msg += g.showUsers();
        }
        return msg;
    }

    /**
     * Check if group already exists
     * @param groupName
     * @return
     */
    private boolean groupExists(String groupName) {
        for(Group g: groups) {
            if(g.getGroupname().equals(groupName)) {
                return true;
            }
        }
        return false;
    }

}
