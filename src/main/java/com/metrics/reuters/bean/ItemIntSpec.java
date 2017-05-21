package com.metrics.reuters.bean;

import java.io.Serializable;
import java.util.List;

public class ItemIntSpec implements Serializable
{
	private List<String> fields = null;
	private String identifiy = null;

	public ItemIntSpec() {
	}

	public ItemIntSpec(String identify, List<String> fields) {
		this.identifiy = identify;
		this.fields = fields;
	}

	public String getIdentifiy() {
		return identifiy;
	}

	public List<String> getFields() {
		return fields;
	}

	public void setIdentifiy(String identifiy) {
		this.identifiy = identifiy;
	}

	public void setFields(List<String> fields) {
		this.fields = fields;
	}

}