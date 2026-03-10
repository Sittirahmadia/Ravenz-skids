package com.raven.ravenz.module.modules.client;

import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.BooleanSetting;

public final class Client extends Module {

    public final BooleanSetting title = new BooleanSetting("Title", true);
    public Client() {
        super("Client", "Settings for the client", -1, Category.CLIENT);

        this.addSettings(
                title
        );
    }

    public boolean getTitle() {
        return title.getValue();
    }

}
