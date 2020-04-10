package gui;

import javax.swing.*;
import java.awt.*;
import java.util.function.Function;

public class SideBar extends JPanel {
    private boolean width;
    private Function<Integer, Integer> function;

    public SideBar(){
        setWidth(false);
    }
    @Override
    protected void paintComponent(Graphics g){
        this.calcSize(function, width);
        super.paintComponent(g);
    }
    private void calcSize(Function<Integer, Integer> scaleFunction, boolean width){
        var pSize = getParent().getSize();
        var dim = (int)((width) ? pSize.getWidth():pSize.getHeight());
        dim /=3;

        if(width){
            setPreferredSize(new Dimension(dim, scaleFunction.apply(dim)));
        } else {
            setPreferredSize(new Dimension(dim, scaleFunction.apply(dim)));
        }
    }

    public void setWidth(boolean b){
        width = b;
        function = b ? (i)-> (int) (i * (1 /.65)): i-> (int) (i * .65);
    }
}
