package me.raid.printjartest.format;

import java.util.HashMap;

import com.laiqian.print.model.PrintContent;
import com.laiqian.print.util.PrintUtils;

public abstract class BaseFormatedBuilder implements IFormatedPrintContentBuilder {
	private PrintContent.Builder mBuilder;
	private int width = 58;
	private HashMap<Integer, int[]> settings = new HashMap<Integer, int[]>();
	private HashMap<Integer, int[]> aligns = new HashMap<Integer, int[]>();
	private int size = PrintContent.FONT_NORMAL;
	private int decimal = 1;
	
	public BaseFormatedBuilder() {
		mBuilder = new PrintContent.Builder();
	}
	
	public BaseFormatedBuilder(PrintContent.Builder builder) {
		mBuilder = builder;
	}
	
	public PrintContent.Builder getBuilder() {
		return mBuilder;
	}
	
	public void setWidth(int width) {
		this.width = width;
	}
	
	public int getWidth() {
		return width;
	}
	
	@Override
	public void setDecimal(int decimal) {
		this.decimal = decimal;
	}
	
	@Override
	public int getDecimal() {
		return decimal;
	}
	
	@Override
	public PrintContent build() {
		return mBuilder.build();
	}
	
	@Override
	public void appendTitle(String title) {
		mBuilder.appendString(title, PrintContent.FONT_DOUBLE_BOTH, PrintContent.ALIGN_CENTER);
	}
	
	@Override
	public void appendString(String str) {
		mBuilder.appendString(str);
	}
	
	public void appendDivider(char ch) {
		int n = width == 58 ? 32 : 44;
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < n; ++i) {
			sb.append(ch);
		}
		getBuilder().appendString(sb.toString());
	}
	
	public void changeSetting(int[] setting) {
		if(setting == null) {
			throw new NullPointerException("parameter cannot be null");
		}
		settings.put(setting.length, setting);
	}
	
	/**
	 * @param align arrays of PrintContent.ALIGN_*, dosen't support CENTER
	 */
	public void changeAlign(int[] align) {
		if(align == null) {
			throw new NullPointerException("parameter cannot be null");
		}
		aligns.put(align.length, align);
	}
	
	public void changeSize(int size) {
		this.size = size;
	}
	
	public int[] getDefaultSetting(int n) {
		int paperLength = PrintUtils.getWidthLength(width);
		int slice = paperLength / n;
		int[] setting = new int[n];
		for(int i = 0; i < n; ++i) {
			setting[i] = slice;
		}
		return setting;
	}
	
	public int[] getDefaultAlign(int n) {
		int[] align = new int[n];
		for(int i = 0; i < n; ++i) {
			align[i] = i == 0 ? PrintContent.ALIGN_LEFT : PrintContent.ALIGN_RIGHT;
		}
		return align;
	}
	
	@Override
	public int getSize() {
		return size;
	}
	
	@Override
	public void setSize(int size) {
		this.size = size;
	}
	
	public int[] getSetting(int n) {
		int[] setting = settings.get(n);
		if(setting == null) {
			setting = getDefaultSetting(n);
		}
		return setting;
	}
	
	public int[] getAlign(int n) {
		int[] align = aligns.get(n);
		if(align == null) {
			align = getDefaultAlign(n);
		}
		return align;
	}
	
}
