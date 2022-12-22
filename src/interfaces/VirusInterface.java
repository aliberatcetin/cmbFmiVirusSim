package interfaces;

import core.NetworkInterface;
import core.Settings;

public class VirusInterface extends SimpleBroadcastInterface{
    public VirusInterface(Settings s) {
        super(s);
    }

    public VirusInterface(SimpleBroadcastInterface ni) {
        super(ni);
    }

    @Override
    protected boolean isWithinRange(NetworkInterface anotherInterface) {
        double smallerRange = anotherInterface.getTransmitRange();
        double myRange = getTransmitRange();
        if (myRange < smallerRange) {
            smallerRange = myRange;
        }

        return this.host.getLocation().distance(
                anotherInterface.getHost().getLocation()) <= smallerRange;
    }
}
