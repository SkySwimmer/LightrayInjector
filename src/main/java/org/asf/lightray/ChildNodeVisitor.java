package org.asf.lightray;

import java.util.HashMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import pxb.android.axml.NodeVisitor;

public class ChildNodeVisitor extends NodeVisitor {

	private Document doc;
	private Node thisNode;

	private HashMap<String, String> namespaces;

	public ChildNodeVisitor(HashMap<String, String> namespaces, String ns, String name, Document newDoc, Node parent) {
		doc = newDoc;
		thisNode = newDoc.createElement((ns == null ? "" : ns + ":") + name);
		this.namespaces = namespaces;
		parent.appendChild(thisNode);
	}

	@Override
	public void attr(String ns, String name, int resourceId, int type, Object obj) {
		super.attr(ns, name, resourceId, type, obj);

		// Compute name
		name = (ns != null ? namespaces.getOrDefault(ns, ns) + ":" : "") + name;

		// Handle type
		String val = null;
		switch (type) {

		// Int
		case NodeVisitor.TYPE_FIRST_INT: {
			val = Integer.toString((int) obj);
			break;
		}

		// Boolean
		case NodeVisitor.TYPE_INT_BOOLEAN: {
			val = (boolean) obj == true ? "true" : "false";
			break;
		}

		case NodeVisitor.TYPE_INT_HEX: {
			val = "0x" + Integer.toString((int) obj, 16);
			break;
		}

		case NodeVisitor.TYPE_REFERENCE: {
			val = "@id/0x" + Integer.toString((int) obj, 16).toUpperCase();
			break;
		}

		// String
		case NodeVisitor.TYPE_STRING: {
			val = (String) obj;
			break;
		}

		}

		// Set
		if (val != null && thisNode instanceof Element) {
			((Element) thisNode).setAttribute(name, val); 
		}
	}

	@Override
	public void text(int lineNumber, String value) {
		super.text(lineNumber, value);
	}

	@Override
	public NodeVisitor child(String ns, String name) {
		return new ChildNodeVisitor(namespaces, ns, name, doc, thisNode);
	}

}
