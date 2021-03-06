package com.bytezone.diskbrowser.disk;

import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.JPanel;

import com.bytezone.diskbrowser.gui.DataSource;
import com.bytezone.diskbrowser.utilities.HexFormatter;

// -----------------------------------------------------------------------------------//
public class DefaultDataSource implements DataSource
// -----------------------------------------------------------------------------------//
{
  public String text;
  byte[] buffer;

  // ---------------------------------------------------------------------------------//
  public DefaultDataSource (String text)
  // ---------------------------------------------------------------------------------//
  {
    this.text = text;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getAssembler ()
  // ---------------------------------------------------------------------------------//
  {
    return null;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getHexDump ()
  // ---------------------------------------------------------------------------------//
  {
    if (buffer != null)
      return HexFormatter.format (buffer, 0, buffer.length);
    return null;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public BufferedImage getImage ()
  // ---------------------------------------------------------------------------------//
  {
    return null;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getText ()
  // ---------------------------------------------------------------------------------//
  {
    return text;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public JComponent getComponent ()
  // ---------------------------------------------------------------------------------//
  {
    System.out.println ("In DefaultDataSource.getComponent()");
    JPanel panel = new JPanel ();
    return panel;
  }
}