package nl.saxion.internettech;

import java.util.ArrayList;

public class GroupManager {
    private ArrayList<Group> groups;

    public GroupManager() {
        groups = new ArrayList<>();
    }

    public String addGroup(Group group) {
        if(!groupExists(group.getGroupname())) {
            groups.add(group);
            return "+OK group added";
        } else {
            return "-ERR group already exists";
        }
    }

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

    public String leaveGroup(String groupname, Server.ClientThread ct) {
        if(groupExists(groupname)) {
            for(Group g: groups) {
                if(g.getGroupname().equals(groupname)) {
                    switch(g.leaveGroup(ct)) {
                        case "DEL":
                            if(deleteGroup(g)) {
                                return "+OK group deleted";
                            } else {
                                return "-ERR ";
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

    public boolean deleteGroup(Group group) {
        for(Group g: groups) {
            if(g.getGroupname().equals(group.getGroupname())) {
                groups.remove(group);
                return true;
            }
        }
        return false;
    }

    public String sendGroupMessage(String[] splits, String username) {
        String groupname = splits[1];
        String msg = "(" + groupname + ") " + username + " says: ";
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

    public String showGroups() {
        String msg = "";
        for(Group g: groups) {
            msg += g.getGroupname() + "\n";
            msg += g.showUsers();
        }
        return msg;
    }

    private boolean groupExists(String groupName) {
        for(Group g: groups) {
            if(g.getGroupname().equals(groupName)) {
                return true;
            }
        }
        return false;
    }

}
