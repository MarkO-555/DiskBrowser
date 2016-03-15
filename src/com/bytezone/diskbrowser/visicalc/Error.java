package com.bytezone.diskbrowser.visicalc;

class Error extends Function
{
  public Error (Sheet parent, String text)
  {
    super (parent, text);
  }

  @Override
  public boolean hasValue ()
  {
    return false;
  }

  @Override
  public String getError ()
  {
    return "@Error";
  }

  @Override
  public double getValue ()
  {
    return 0;
  }
}