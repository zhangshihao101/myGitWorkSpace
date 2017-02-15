package me.raid.printjartest.format;

import com.laiqian.print.model.PrintContent;
import com.laiqian.print.util.PrintUtils;

public class FixedPositionPrintContentBuilder extends BaseFormatedBuilder {
	private int width = 58;
	
	public FixedPositionPrintContentBuilder() {
		super();
		setSettings();
	}
	
	public FixedPositionPrintContentBuilder(PrintContent.Builder builder) {
		super(builder);
		setSettings();
	}
	
	@Override
	public void setWidth(int width) {
		super.setWidth(width);
		setSettings();
	}
	
	private void setSettings() {
		int paperLength = PrintUtils.getWidthLength(getSize());
		int slice;
		changeSetting(new int[] {0});
		changeSetting(new int[] {0, paperLength - 1});
		slice = paperLength / 3;
		changeSetting(new int[] {0, slice * 2, paperLength - 1});
		slice = paperLength / 4;
		changeSetting(new int[] {0, slice * 2, slice * 3, paperLength - 1});
	}
	
	@Override
	public void appendStrings(String... strings) {
		int length = strings.length;
		if(length == 0) {
			return;
		}
		PrintUtils.appendFixedPositionString(getBuilder(), width, getSetting(length), getAlign(length), strings, 
				getSize());
	}
	
}
