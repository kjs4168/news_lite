package team3.gui.IconButton;

import javax.swing.*;

public class buttonAddKeywordToList extends iconButtonFactory {
    public buttonAddKeywordToList(ImageIcon icon, int x, int y) {
        super(icon, x, y);
    }

    @Override
    public void buttonSetting(JButton _btn) {
        _btn.setIcon(_icon);
        _btn.setBackground(_color);
        _btn.setBounds(x_axis, y_axis, _width, _height);
    }
}