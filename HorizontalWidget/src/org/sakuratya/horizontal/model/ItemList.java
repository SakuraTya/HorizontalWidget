package org.sakuratya.horizontal.model;

import java.io.Serializable;
import java.util.ArrayList;

public class ItemList implements Serializable {

	private static final long serialVersionUID = -514170357600555210L;
	
	public int count;
	public int num_pages;
	public ArrayList<Item> objects;
	public String slug;
	public String title;
}
