package me.raid.printjartest.format;

import com.laiqian.print.model.PrintContent;
import com.laiqian.print.util.PrintUtils;

public class FixedSpacePrintContentBuilder extends BaseFormatedBuilder {
	
	public FixedSpacePrintContentBuilder() {
		super();
		setSettings();
	}
	
	public FixedSpacePrintContentBuilder(PrintContent.Builder builder) {
		super(builder);
		setSettings();
	}
	
	@Override
	public void setWidth(int width) {
		super.setWidth(width);
		setSettings();
	}
	
	private void setSettings() {
		int paperLength = PrintUtils.getWidthLength(getWidth());
		int first;
		int second;
		int third;
		int fourth;
		
		first = paperLength * 1 / 2 + 1;
		second = paperLength - first;
		changeSetting(new int[] {first, second});
		
		first = (int)(paperLength * 7.0 / 17);
		third = (int)(paperLength * 7.0 / 17);
		second = paperLength - first - third;
		changeSetting(new int[] {first, second, third});
		
		first = paperLength / 3;
		second = paperLength / 5;
		third = second;
		fourth = paperLength - first - second - third;
		changeSetting(new int[] {first, second, third, fourth});
	}
	
	@Override
	public void appendStrings(String... strings) {
		int length = strings.length;
		if(length == 0) {
			return;
		}
		PrintUtils.appendFixedSpaceString(getBuilder(), getSetting(length), getAlign(length), strings, getSize());
	}
	
}
