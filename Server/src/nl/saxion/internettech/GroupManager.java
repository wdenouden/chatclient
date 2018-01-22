package nl.saxion.internettech;

import java.util.ArrayList;

public class GroupManager {

    private ArrayList<Group> groups;

    public GroupManager() {
        groups = new ArrayList<Group>();
    }

    public void addGroup(Group group) {
        groups.add(group);
    }

    public ArrayList<Group> getGroups() {
        return groups;
    }

    public void joinGroup() {

    }

    public void leaveGroup() {

    }

    public void kickUser() {

    }
}