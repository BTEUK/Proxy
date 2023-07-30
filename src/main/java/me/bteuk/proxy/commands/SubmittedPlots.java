package me.bteuk.proxy.commands;

public class SubmittedPlots extends PlotListCommand {

    /**
     * Constructor, saved the name and description of the command.
     * Also registers the command in Discord.
     *
     * @param name        Name of the command
     * @param description Description of the command
     */
    public SubmittedPlots(String name, String description) {
        super(name, description, "Submitted Plots", "SELECT id FROM plot_data WHERE status='submitted' OR status='reviewing';", "There are currently no submitted plots.");
    }
}
