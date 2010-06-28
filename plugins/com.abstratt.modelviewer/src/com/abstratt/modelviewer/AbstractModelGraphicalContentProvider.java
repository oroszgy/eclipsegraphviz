/*******************************************************************************
 * Copyright (c) 2007 EclipseGraphviz contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     abstratt technologies
 *******************************************************************************/
package com.abstratt.modelviewer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

import com.abstratt.graphviz.DOTRenderingUtils;
import com.abstratt.graphviz.ui.DOTGraphicalContentProvider;
import com.abstratt.modelrenderer.IRendererSelector;
import com.abstratt.modelrenderer.IRenderingSession;
import com.abstratt.modelrenderer.IndentedPrintWriter;
import com.abstratt.modelrenderer.RenderingSession;
import com.abstratt.pluginutils.LogUtils;

public abstract class AbstractModelGraphicalContentProvider extends
		DOTGraphicalContentProvider {

	private static String ID = AbstractModelGraphicalContentProvider.class
			.getPackage().getName();

	public static void logUnexpected(String message, Exception e) {
		LogUtils.logError(ID, message, e);
	}

	private static String toMB(long byteCount) {
		return byteCount / (1024 * 1024) + "MB";
	}

	protected abstract IRendererSelector<?, ?> getRendererSelector();

	/*
	 * (non-Javadoc)
	 * @see com.abstratt.graphviz.ui.DOTGraphicalContentProvider#loadImage(org.eclipse.swt.widgets.Display, org.eclipse.swt.graphics.Point, java.lang.Object)
	 */
	public Image loadImage(Display display, Point desiredSize, Object newInput) throws CoreException {
		if (newInput == null)
			// for a reload, it will be null
			return new Image(display, 1, 1);
		byte[] dotContents = generateDOTFromModel((URI) newInput);
		if (dotContents != null)
			return super.loadImage(display, desiredSize, dotContents);
		return new Image(display, 1, 1);
	}
	
	@Override
	public void saveImage(Display display, Point suggestedSize, Object input, IPath outputLocation, int fileFormat)
					throws CoreException {
		byte[] dotContents = generateDOTFromModel((URI) input);
		if (dotContents == null)
			dotContents = new byte[0];
		super.saveImage(display, suggestedSize, dotContents, outputLocation, fileFormat);
	}

	private byte[] generateDOTFromModel(URI modelURI) throws CoreException {
		org.eclipse.emf.common.util.URI emfURI = org.eclipse.emf.common.util.URI
				.createURI(modelURI.toASCIIString());
		ResourceSet resourceSet = new ResourceSetImpl();
		Resource resource = resourceSet.createResource(emfURI);
		try {
			// TODO cache option map and use XMLResource constants (requires
			// dependency change)
			Map<String, Object> options = new HashMap<String, Object>();
			options.put("DISABLE_NOTIFY", Boolean.TRUE);
			resource.load(options);
			Collection<EObject> contents = resource.getContents();
			IRendererSelector<?, ?> selector = getRendererSelector();
			StringWriter sw = new StringWriter();
			IndentedPrintWriter out = new IndentedPrintWriter(sw);
			IRenderingSession session = new RenderingSession(selector,
					out);
			printPrologue(emfURI.trimFileExtension().lastSegment(), out);
			session.renderAll(contents);
			printEpilogue(out);
			out.close();
			byte[] dotContents = sw.getBuffer().toString().getBytes();
			if (Boolean.getBoolean(ID + ".showDOT"))
			{
				LogUtils.log(IStatus.INFO, ID, "DOT output for " + modelURI, null);
				LogUtils.log(IStatus.INFO, ID, sw.getBuffer().toString(), null);
			}
			if (Boolean.getBoolean(ID + ".showMemory"))
				System.out.println("*** free: "
						+ toMB(Runtime.getRuntime().freeMemory())
						+ " / total: "
						+ toMB(Runtime.getRuntime().totalMemory()) + " / max: "
						+ toMB(Runtime.getRuntime().maxMemory()));
			return dotContents;
		} catch (FileNotFoundException e) {
			// file was deleted before we could read it - that is alright, don't
			// make a fuss
			return null;
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, ID, "", e));
		} catch (RuntimeException e) {
			// invalid file formats might cause runtime exceptions
			throw new CoreException(new Status(IStatus.ERROR, ID, "", e));
		} finally {
			try {
				unloadResources(resourceSet);
			} catch (RuntimeException re) {
				logUnexpected("Unloading resources", re);
			}
		}
	}

	private void printEpilogue(IndentedPrintWriter w) {
		w.exitLevel();
		w.println();
		w.println("}"); //$NON-NLS-1$
	}

	private void printPrologue(String modelName, IndentedPrintWriter w) {
		w.println("graph " + modelName + " {"); //$NON-NLS-1$ //$NON-NLS-2$
		w.enterLevel();
		DOTRenderingUtils.addAttribute(w, "ranksep", "0.8");
		DOTRenderingUtils.addAttribute(w, "nodesep", "0.85");
		DOTRenderingUtils.addAttribute(w, "nojustify", "true");
		w.println("graph [");
		w.enterLevel();
		// DOTRenderingUtils.addAttribute(w, "outputorder", "edgesfirst");
		// DOTRenderingUtils.addAttribute(w, "packmode", "graph");
		// DOTRenderingUtils.addAttribute(w, "pack", 40);
		// DOTRenderingUtils.addAttribute(w, "ratio", "auto");
		// DOTRenderingUtils.addAttribute(w, "rank", "sink");
		// DOTRenderingUtils.addAttribute(w, "overlap", "ipsep");
		w.exitLevel();
		w.println("]");
		// TODO provide choice
		w.println("node [");
		w.enterLevel();
		DOTRenderingUtils.addAttribute(w, "fontsize", 12);
		DOTRenderingUtils.addAttribute(w, "shape", "plaintext");
		w.exitLevel();
		w.println("]");
		w.println("edge [");
		w.enterLevel();
		DOTRenderingUtils.addAttribute(w, "fontsize", 9);
		// DOTRenderingUtils.addAttribute(w, "splines", "polyline");
		w.exitLevel();
		w.println("]");
	}
	
	private static void unloadResources(ResourceSet resourceSet) {
		for (Resource current : resourceSet.getResources())
			current.unload();
	}
}
