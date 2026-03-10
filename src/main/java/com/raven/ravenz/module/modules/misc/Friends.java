package com.raven.ravenz.module.modules.misc;

import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.gui.FriendsScreen;

public class Friends extends Module {

    public Friends() {
        super("Friends", "Manage your friends list", Category.MISC);
    }

    @Override
    public void onEnable() {
        if (mc.player != null) {
            mc.setScreen(new FriendsScreen());
        }
        setEnabled(false);
    }
}
