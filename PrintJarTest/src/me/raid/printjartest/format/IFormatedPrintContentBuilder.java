package me.raid.printjartest.format;

import com.laiqian.print.model.PrintContent;

public interface IFormatedPrintContentBuilder {
	public PrintContent.Builder getBuilder();
	public PrintContent build();
	public void setWidth(int width);
	public int getWidth();
	public void setSize(int size);
	public int getSize();
	public void setDecimal(int decimal);
	public int getDecimal();
	public void appendTitle(String title);
	public void appendDivider(char ch);
	public void appendString(String str);
	public void appendStrings(String... strings);
}
