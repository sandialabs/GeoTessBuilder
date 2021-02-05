//- ****************************************************************************
//-
//- Copyright 2009 Sandia Corporation. Under the terms of Contract
//- DE-AC04-94AL85000 with Sandia Corporation, the U.S. Government
//- retains certain rights in this software.
//-
//- BSD Open Source License.
//- All rights reserved.
//-
//- Redistribution and use in source and binary forms, with or without
//- modification, are permitted provided that the following conditions are met:
//-
//-    * Redistributions of source code must retain the above copyright notice,
//-      this list of conditions and the following disclaimer.
//-    * Redistributions in binary form must reproduce the above copyright
//-      notice, this list of conditions and the following disclaimer in the
//-      documentation and/or other materials provided with the distribution.
//-    * Neither the name of Sandia National Laboratories nor the names of its
//-      contributors may be used to endorse or promote products derived from
//-      this software without specific prior written permission.
//-
//- THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
//- AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
//- IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
//- ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
//- LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
//- CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
//- SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
//- INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
//- CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
//- ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
//- POSSIBILITY OF SUCH DAMAGE.
//-
//- ****************************************************************************

package geotessbuildertest;

import gov.sandia.geotess.*;
import gov.sandia.geotessbuilder.GeoTessBuilderMain;
import gov.sandia.gmp.util.containers.hash.sets.HashSetInteger;
import gov.sandia.gmp.util.containers.hash.sets.HashSetInteger.Iterator;
import gov.sandia.gmp.util.globals.DataType;
import gov.sandia.gmp.util.numerical.polygon.Horizon;
import gov.sandia.gmp.util.numerical.polygon.HorizonLayer;
import gov.sandia.gmp.util.numerical.polygon.Polygon;
import gov.sandia.gmp.util.numerical.polygon.Polygon3D;
import gov.sandia.gmp.util.numerical.vector.VectorGeo;
import gov.sandia.gmp.util.numerical.vector.VectorUnit;
import gov.sandia.gmp.util.propertiesplus.PropertiesPlus;
import org.junit.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RefineModelTest {

	/**
	 * If verbosity is zero, no output to screen. The bigger verbosity, the more
	 * output is generated.
	 */
	private static int verbosity = 0;

	/**
	 * Tests can generate vtk files of the original grid, the new grid, and the
	 * number of points in each grid. Only generated if generateVtkFiles is true
	 */
	private static boolean generateVtkFiles = false;

	/**
	 * Directory where vtk files will be written. outputDir must exist but vtkDir
	 * will be generated if it does not exist.
	 */
	private static File outputDir = new File("/geotess/Error_in_GeoTessBuilderMain/2021-01-10");
	private static File vtkDir = new File(outputDir, "vtk");

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		if (generateVtkFiles && outputDir.exists())
			vtkDir.mkdir();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test1() throws Exception {

		if (verbosity > 0)
			System.out.println("************************************************\n" + "test1()\n");

		GeoTessModel model1 = getGlobalModel();

		int refinedLayer = 0;
		int refinedVertex = 0;
		int refinedNode = 1;

		int refinedPoint = model1.getPointMap().getPointIndex(refinedVertex, refinedLayer, refinedNode);

		PropertiesPlus properties = new PropertiesPlus();
		properties.setProperty("verbosity", verbosity);
		properties.setProperty("gridConstructionMode = model refinement");
		properties.setProperty("pointsToRefine = " + refinedPoint);
		if (generateVtkFiles)
			properties.setProperty("vtkDir", new File(vtkDir, "test1").getCanonicalPath());

		GeoTessModel model2 = (GeoTessModel) GeoTessBuilderMain.run(properties, model1);

		// test that vertex[0] is located at the north pole.
		assertTrue(VectorUnit.dot(model2.getGrid().getVertex(0), new double[] { 0, 0, 1 }) > Math.cos(1e-7));

		// test that each tessellation has the expected number of vertices.
		assertEquals(12, model1.getGrid().getVertexIndicesTopLevel(0).size());
		assertEquals(22, model2.getGrid().getVertexIndicesTopLevel(0).size());

		assertEquals(42, model1.getGrid().getVertexIndicesTopLevel(1).size());
		assertEquals(42, model2.getGrid().getVertexIndicesTopLevel(1).size());

		assertEquals(162, model1.getGrid().getVertexIndicesTopLevel(2).size());
		assertEquals(162, model2.getGrid().getVertexIndicesTopLevel(2).size());

		Iterator it = null;

		int refinedTessellation = model1.getMetaData().getTessellation(refinedLayer);

		// right answer:
		// it =
		// model2.getGrid().getVertexIndicesTopLevel(refinedTessellation).iterator();
		// while (it.hasNext())
		// {
		// int v=it.next();
		// if (model2.getProfile(v, refinedLayer).getNData() > 4)
		// System.out.printf(", %d", v);
		// }

		// we expect that the following vertices will have been refined in refinedLayer
		HashSet<Integer> refinedVertices = new HashSet<Integer>();
		for (int i : new int[] { 0, 13, 14, 16, 18, 20 })
			refinedVertices.add(i);

		// iterate over all the connected vertices in refinedLayer and ensure that they
		// have the right number of nodes.
		it = model2.getGrid().getVertexIndicesTopLevel(refinedTessellation).iterator();
		while (it.hasNext()) {
			int v = it.next();
			if (refinedVertices.contains(v))
				assertEquals(6, model2.getProfile(v, refinedLayer).getNData());
			else
				assertEquals(4, model2.getProfile(v, refinedLayer).getNData());
		}

	}

	@Test
	public void test2() throws Exception {

		if (verbosity > 0)
			System.out.println("************************************************\n" + "test2()\n");

		GeoTessModel model1 = getGlobalModel();

		Horizon bottom = new HorizonLayer(0., 1);
		Horizon top = new HorizonLayer(1., 1);
		Polygon3D polygon = new Polygon3D(true, bottom, top);

		model1.setActiveRegion(polygon);

		int refinedLayer = 1;
		int refinedVertex = 0;
		int refinedNode = 1;

		int refinedPoint = model1.getPointMap().getPointIndex(refinedVertex, refinedLayer, refinedNode);

		PropertiesPlus properties = new PropertiesPlus();
		properties.setProperty("verbosity", verbosity);
		properties.setProperty("gridConstructionMode = model refinement");
		properties.setProperty("pointsToRefine = " + refinedPoint);
		if (generateVtkFiles)
			properties.setProperty("vtkDir", new File(vtkDir, "test2").getCanonicalPath());

		GeoTessModel model2 = (GeoTessModel) GeoTessBuilderMain.run(properties, model1);

		// test that vertex[0] is located at the north pole.
		assertTrue(VectorUnit.dot(model2.getGrid().getVertex(0), new double[] { 0, 0, 1 }) > Math.cos(1e-7));

		// test that each tessellation has the expected number of vertices.
		assertEquals(12, model1.getGrid().getVertexIndicesTopLevel(0).size());
		assertEquals(12, model2.getGrid().getVertexIndicesTopLevel(0).size());

		assertEquals(42, model1.getGrid().getVertexIndicesTopLevel(1).size());
		assertEquals(52, model2.getGrid().getVertexIndicesTopLevel(1).size());

		assertEquals(162, model1.getGrid().getVertexIndicesTopLevel(2).size());
		assertEquals(162, model2.getGrid().getVertexIndicesTopLevel(2).size());

		Iterator it = null;

		int refinedTessellation = model1.getMetaData().getTessellation(refinedLayer);

		// right answer:
		// it =
		// model2.getGrid().getVertexIndicesTopLevel(refinedTessellation).iterator();
		// while (it.hasNext())
		// {
		// int v=it.next();
		// if (model2.getProfile(v, refinedLayer).getNData() > 4)
		// System.out.printf(", %d", v);
		// }

		// we expect that the following vertices will have been refined in refinedLayer
		HashSet<Integer> refinedVertices = new HashSet<Integer>();
		for (int i : new int[] { 0, 46, 45, 54, 61, 68 })
			refinedVertices.add(i);

		// iterate over all the connected vertices in refinedLayer and ensure that they
		// have the right number of nodes.
		it = model2.getGrid().getVertexIndicesTopLevel(refinedTessellation).iterator();
		while (it.hasNext()) {
			int v = it.next();
			if (refinedVertices.contains(v))
				assertEquals(6, model2.getProfile(v, refinedLayer).getNData());
			else
				assertEquals(4, model2.getProfile(v, refinedLayer).getNData());
		}

	}

	@Test
	public void test3() throws Exception {

		if (verbosity > 0)
			System.out.println("************************************************\n" + "test3()\n");

		GeoTessModel model1 = getGlobalModel();

		int refinedLayer = 2;
		int refinedVertex = 0;
		int refinedNode = 3;

		int refinedPoint = model1.getPointMap().getPointIndex(refinedVertex, refinedLayer, refinedNode);

		PropertiesPlus properties = new PropertiesPlus();
		properties.setProperty("verbosity", verbosity);
		properties.setProperty("gridConstructionMode = model refinement");
		properties.setProperty("pointsToRefine = " + refinedPoint);
		if (generateVtkFiles)
			properties.setProperty("vtkDir", new File(vtkDir, "test3").getCanonicalPath());

		GeoTessModel model2 = (GeoTessModel) GeoTessBuilderMain.run(properties, model1);

		// test that vertex[0] is located at the north pole.
		assertTrue(VectorUnit.dot(model2.getGrid().getVertex(0), new double[] { 0, 0, 1 }) > Math.cos(1e-7));

		// test that each tessellation has the expected number of vertices.
		assertEquals(12, model1.getGrid().getVertexIndicesTopLevel(0).size());
		assertEquals(12, model2.getGrid().getVertexIndicesTopLevel(0).size());

		assertEquals(42, model1.getGrid().getVertexIndicesTopLevel(1).size());
		assertEquals(42, model2.getGrid().getVertexIndicesTopLevel(1).size());

		assertEquals(162, model1.getGrid().getVertexIndicesTopLevel(2).size());
		assertEquals(172, model2.getGrid().getVertexIndicesTopLevel(2).size());

		Iterator it = null;

		int refinedTessellation = model1.getMetaData().getTessellation(refinedLayer);

		// right answer:
		// it =
		// model2.getGrid().getVertexIndicesTopLevel(refinedTessellation).iterator();
		// while (it.hasNext())
		// {
		// int v=it.next();
		// if (model2.getProfile(v, refinedLayer).getNData() > 4)
		// System.out.printf(", %d", v);
		// }

		// we expect that the following vertices will have been refined in refinedLayer
		HashSet<Integer> refinedVertices = new HashSet<Integer>();
		for (int i : new int[] { 0, 170, 168, 163, 166, 164 })
			refinedVertices.add(i);

		// iterate over all the connected vertices in refinedLayer and ensure that they
		// have the right number of nodes.
		it = model2.getGrid().getVertexIndicesTopLevel(refinedTessellation).iterator();
		while (it.hasNext()) {
			int v = it.next();
			if (refinedVertices.contains(v))
				assertEquals(5, model2.getProfile(v, refinedLayer).getNData());
			else
				assertEquals(4, model2.getProfile(v, refinedLayer).getNData());
		}

	}

	@Test
	public void test4() throws Exception {

		if (verbosity > 0)
			System.out.println("************************************************\n" + "test4()\n");

		GeoTessModel model1 = getGlobalModel();

		int refinedLayer = 2;

		Horizon bottom = new HorizonLayer(0., refinedLayer);
		Horizon top = new HorizonLayer(1., refinedLayer);
		Polygon3D polygon = new Polygon3D(new double[] { 0., 0., 1 }, Math.toRadians(20), 100, bottom, top);

		model1.setActiveRegion(polygon);

		PropertiesPlus properties = new PropertiesPlus();
		properties.setProperty("verbosity", verbosity);
		properties.setProperty("gridConstructionMode = model refinement");
		properties.setProperty("threshold = DATA > 1");
		if (generateVtkFiles)
			properties.setProperty("vtkDir", new File(vtkDir, "test4").getCanonicalPath());

		GeoTessModel model2 = (GeoTessModel) GeoTessBuilderMain.run(properties, model1);

		// test that vertex[0] is located at the north pole.
		assertTrue(VectorUnit.dot(model2.getGrid().getVertex(0), new double[] { 0, 0, 1 }) > Math.cos(1e-7));

		// test that each tessellation has the expected number of vertices.
		assertEquals(12, model1.getGrid().getVertexIndicesTopLevel(0).size());
		assertEquals(12, model2.getGrid().getVertexIndicesTopLevel(0).size());

		assertEquals(42, model1.getGrid().getVertexIndicesTopLevel(1).size());
		assertEquals(42, model2.getGrid().getVertexIndicesTopLevel(1).size());

		assertEquals(162, model1.getGrid().getVertexIndicesTopLevel(2).size());
		assertEquals(197, model2.getGrid().getVertexIndicesTopLevel(2).size());

		Iterator it = null;

		int refinedTessellation = model1.getMetaData().getTessellation(refinedLayer);

		// right answer:
		// it =
		// model2.getGrid().getVertexIndicesTopLevel(refinedTessellation).iterator();
		// while (it.hasNext())
		// {
		// int v=it.next();
		// if (model2.getProfile(v, refinedLayer).getNData() > 4)
		// System.out.printf(", %d", v);
		// }

		// we expect that the following vertices will have been refined in refinedLayer
		HashSet<Integer> refinedVertices = new HashSet<Integer>();
		for (int i : new int[] { 0, 46, 45, 54, 61, 68, 171, 170, 175, 174, 173, 172, 163, 162, 167, 166, 165, 164, 186,
				187, 185, 188, 189, 178, 179, 182, 180, 181, 193, 192, 194 })
			refinedVertices.add(i);

		// iterate over all the connected vertices in refinedLayer and ensure that they
		// have the right number of nodes.
		it = model2.getGrid().getVertexIndicesTopLevel(refinedTessellation).iterator();
		while (it.hasNext()) {
			int v = it.next();
			if (refinedVertices.contains(v))
				assertEquals(7, model2.getProfile(v, refinedLayer).getNData());
			else
				assertEquals(4, model2.getProfile(v, refinedLayer).getNData());
		}

	}

	@Test
	public void test5() throws Exception {

		if (verbosity > 0)
			System.out.println("************************************************\n" + "test5()\n");

		GeoTessModel model1 = getGlobalModel(30, 30);

		int refinedLayer = 2;

		Horizon bottom = new HorizonLayer(0., refinedLayer);
		Horizon top = new HorizonLayer(1., refinedLayer);

		// define circular polygon centered on the north pole. 20 degree radius
		Polygon3D polygon = new Polygon3D(new double[] { 0., 0., 1 }, Math.toRadians(20), 100, bottom, top);

		model1.setActiveRegion(polygon);

		PropertiesPlus properties = new PropertiesPlus();
		properties.setProperty("verbosity", verbosity);
		properties.setProperty("gridConstructionMode = model refinement");
		properties.setProperty("threshold = DATA > 1");
		if (generateVtkFiles)
			properties.setProperty("vtkDir", new File(vtkDir, "test5").getCanonicalPath());

		GeoTessModel model2 = (GeoTessModel) GeoTessBuilderMain.run(properties, model1);

		// test that vertex[0] is located at [30, 30].
		assertTrue(VectorUnit.dot(model2.getGrid().getVertex(0), VectorGeo.getVectorDegrees(30, 30)) > Math.cos(1e-7));

		// test that each tessellation has the expected number of vertices.
		assertEquals(12, model1.getGrid().getVertexIndicesTopLevel(0).size());
		assertEquals(12, model2.getGrid().getVertexIndicesTopLevel(0).size());

		assertEquals(42, model1.getGrid().getVertexIndicesTopLevel(1).size());
		assertEquals(42, model2.getGrid().getVertexIndicesTopLevel(1).size());

		assertEquals(162, model1.getGrid().getVertexIndicesTopLevel(2).size());
		assertEquals(197, model2.getGrid().getVertexIndicesTopLevel(2).size());

		Iterator it = null;

		int refinedTessellation = model1.getMetaData().getTessellation(refinedLayer);

		// right answer:
		// it =
		// model2.getGrid().getVertexIndicesTopLevel(refinedTessellation).iterator();
		// while (it.hasNext())
		// {
		// int v=it.next();
		// if (model2.getProfile(v, refinedLayer).getNData() > 4)
		// System.out.printf(", %d", v);
		// }

		// we expect that the following vertices will have been refined in refinedLayer
		HashSet<Integer> refinedVertices = new HashSet<Integer>();
		for (int i : new int[] { 2, 50, 49, 75, 83, 108, 171, 170, 175, 174, 173, 172, 163, 162, 167, 166, 165, 164,
				186, 187, 184, 185, 188, 189, 178, 179, 180, 181, 193, 192, 194 })
			refinedVertices.add(i);

		// iterate over all the connected vertices in refinedLayer and ensure that they
		// have the right number of nodes.
		it = model2.getGrid().getVertexIndicesTopLevel(refinedTessellation).iterator();
		while (it.hasNext()) {
			int v = it.next();
			if (refinedVertices.contains(v))
				assertEquals(7, model2.getProfile(v, refinedLayer).getNData());
			else
				assertEquals(4, model2.getProfile(v, refinedLayer).getNData());
		}

	}

	@Test
	public void test6() throws Exception {

		if (verbosity > 0)
			System.out.println("************************************************\n" + "test6()\n");

		GeoTessModel model1 = getCrustalModel1Layer();

		int refinedPoint = 0;

		PropertiesPlus properties = new PropertiesPlus();
		properties.setProperty("verbosity", verbosity);
		properties.setProperty("gridConstructionMode = model refinement");
		properties.setProperty("pointsToRefine = " + refinedPoint);
		if (generateVtkFiles)
			properties.setProperty("vtkDir", new File(vtkDir, "test6").getCanonicalPath());

		GeoTessModel model2 = (GeoTessModel) GeoTessBuilderMain.run(properties, model1);

		// this model has only one layer and one tessellation.
		int layer = 0;
		int tessid = 0;

		// test that vertex[0] is located at [20, 20].
		assertTrue(VectorUnit.dot(model2.getGrid().getVertex(0), VectorGeo.getVectorDegrees(20, 20)) > Math.cos(1e-7));

		// test that each tessellation has the expected number of vertices.
		assertEquals(22, model1.getGrid().getVertexIndicesTopLevel(tessid).size());
		assertEquals(30, model2.getGrid().getVertexIndicesTopLevel(tessid).size());

//		// right answer:
//		// print out the vertices that got refined.
//		for (int v=0; v<model2.getNVertices(); ++v)
//			if (model2.getProfile(v, layer).getNData() > 0)
//				System.out.printf(", %d", v);

		// we expect that the following vertices will have been refined
		HashSet<Integer> refinedVertices = new HashSet<Integer>();
		for (int i : new int[] { 0, 23, 24, 26, 28 })
			refinedVertices.add(i);

		// iterate over all the connected vertices and ensure that they
		// have the right number of nodes.
		for (int v = 0; v < model2.getNVertices(); ++v)
			if (refinedVertices.contains(v))
				assertEquals(1, model2.getProfile(v, layer).getNData());
			else
				assertEquals(0, model2.getProfile(v, layer).getNData());
	}

	@Test
	public void test7() throws Exception {

		if (verbosity > 0)
			System.out.println("************************************************\n" + "test7()\n");

		GeoTessModel model1 = getCrustalModel3Layers();

		ArrayList<Integer> pointsToRefine = new ArrayList<Integer>();
		Polygon x = new Polygon(new double[] { 1, 0, 0 }, Math.toRadians(20.), 50);
		Polygon y = new Polygon(new double[] { 0, 1, 0 }, Math.toRadians(20.), 50);
		Polygon z = new Polygon(new double[] { 0, 0, 1 }, Math.toRadians(20.), 50);

		for (int pointIndex = 0; pointIndex < model1.getPointMap().size(); ++pointIndex) {
			double[] vtx = model1.getPointMap().getPointUnitVector(pointIndex);
			int layer = model1.getPointMap().getLayerIndex(pointIndex);
			if (layer == 0 && x.contains(vtx))
				pointsToRefine.add(pointIndex);
			if (layer == 1 && y.contains(vtx))
				pointsToRefine.add(pointIndex);
			if (layer == 2 && z.contains(vtx))
				pointsToRefine.add(pointIndex);
		}
		System.out.println();

		PropertiesPlus properties = new PropertiesPlus();
		properties.setProperty("verbosity", verbosity);
		properties.setProperty("gridConstructionMode = model refinement");
		properties.setProperty("pointsToRefine = " + pointsToRefine.toString());
		if (generateVtkFiles)
			properties.setProperty("vtkDir", new File(vtkDir, "test7").getCanonicalPath());

		GeoTessModel model2 = (GeoTessModel) GeoTessBuilderMain.run(properties, model1);

		// test that vertex[0] is located at the north pole.
		assertTrue(VectorUnit.dot(model2.getGrid().getVertex(0), new double[] { 0, 0, 1 }) > Math.cos(1e-7));

		// test that each tessellation has the expected number of vertices.
		assertEquals(162, model1.getNVertices());
		assertEquals(281, model2.getNVertices());

		// we expect some of the vertices in layer 1 to be refined radially.
		// none of the vertices in layers 0 and 2 should be refined radially.
		int layer = 1;

//		// right answer:
//		// print out the vertices that got refined.
//		for (int v=0; v<model2.getNVertices(); ++v)
//			if (model2.getProfile(v, layer).getNData() > 3)
//				System.out.printf(", %d", v);

		// we expect that the following vertices will have been refined in layer 1
		HashSet<Integer> refinedVertices = new HashSet<Integer>();
		for (int i : new int[] { 24, 85, 86, 89, 90, 117, 118, 208, 209, 210, 211, 212, 213, 216, 217, 218, 219, 221,
				222, 224, 225, 226, 227, 230, 262, 263, 264, 265, 268, 269, 270, 271, 272, 273, 276, 277, 278 })
			refinedVertices.add(i);

		// iterate over all the connected vertices in layer 1 and ensure that they
		// have the right number of nodes.
		for (int v = 0; v < model2.getNVertices(); ++v)
			if (refinedVertices.contains(v))
				assertEquals(5, model2.getProfile(v, layer).getNData());
			else
				assertEquals(3, model2.getProfile(v, layer).getNData());

		// iterate over all the connected vertices in layers 0 and 2 and ensure that
		// they
		// have the right number of nodes.
		for (int v = 0; v < model2.getNVertices(); ++v) {
			assertEquals(1, model2.getProfile(v, 0).getNData());
			assertEquals(1, model2.getProfile(v, 2).getNData());
		}
	}

	@Test
	public void test8() throws Exception {

		if (verbosity > 0)
			System.out.println("************************************************\n" + "test8()\n");

		GeoTessModel model1 = get2DModel();

		File vtk = new File(vtkDir, "test8");

		PropertiesPlus properties = new PropertiesPlus();
		properties.setProperty("verbosity", verbosity);
		properties.setProperty("gridConstructionMode = model refinement");
		properties.setProperty("threshold = DATA > 1");
		if (generateVtkFiles)
			properties.setProperty("vtkDir", vtk.getCanonicalPath());

		GeoTessModel model2 = (GeoTessModel) GeoTessBuilderMain.run(properties, model1);

		File vtkFile = new File(vtk, "datavalues.vtk");

		if (generateVtkFiles)
			GeoTessModelUtils.vtk(model2, vtkFile.getAbsolutePath(), 0, false, null);

		// this model has only one layer and one tessellation.
		int layer = 0;
		int tessid = 0;

		// test that vertex[0] is located at the north pole.
		assertTrue(VectorUnit.dot(model2.getGrid().getVertex(0), new double[] { 0, 0, 1 }) > Math.cos(1e-7));

		// test that each tessellation has the expected number of vertices.
		assertEquals(162, model1.getGrid().getVertexIndicesTopLevel(tessid).size());
		assertEquals(197, model2.getGrid().getVertexIndicesTopLevel(tessid).size());

		// right answer:
		// // print out the vertices that got refined.
		// for (int v=0; v<model2.getNVertices(); ++v)
		// if (model2.getProfile(v, layer).getValue(0, 0) == 1F)
		// System.out.printf(", %d", v);
		// System.out.println();
		// // print out the vertices that got refined.
		// for (int v=0; v<model2.getNVertices(); ++v)
		// if (model2.getProfile(v, layer).getValue(0, 0) == 2F)
		// System.out.printf(", %d", v);
		// System.out.println();

		// we expect that the following vertices will have been refined
		HashSet<Integer> values1 = new HashSet<Integer>();
		for (int i : new int[] { 163, 164, 167, 170, 172, 173, 175, 179, 180, 182, 186, 187, 189, 193, 194 })
			values1.add(i);

		// we expect that the following vertices will have been refined
		HashSet<Integer> values2 = new HashSet<Integer>();
		for (int i : new int[] { 0, 45, 46, 54, 61, 68, 162, 165, 166, 171, 174, 178, 181, 185, 188, 192 })
			values2.add(i);

		// iterate over all the connected vertices and ensure that they
		// have the right number of nodes.
		for (int v = 0; v < model2.getNVertices(); ++v)
			if (values1.contains(v))
				assertEquals(1., model2.getProfile(v, layer).getValue(0, 0), 1e-3);
			else if (values2.contains(v))
				assertEquals(2., model2.getProfile(v, layer).getValue(0, 0), 1e-3);
			else
				assertEquals(0., model2.getProfile(v, layer).getValue(0, 0), 1e-3);
	}

	/**
	 * Generate unrotated model.
	 * 
	 * @return
	 * @throws Exception
	 */
	public GeoTessModel getGlobalModel() throws Exception {
		return getGlobalModel(Double.NaN, Double.NaN);
	}

	/**
	 * Generate a model rotated such that vertex[0] is located at specified point.
	 * 
	 * @param lat point latitude in degrees
	 * @param lon point longitude in degrees
	 * @return
	 * @throws Exception
	 */
	public GeoTessModel getGlobalModel(double lat, double lon) throws Exception {

		// build a grid
		PropertiesPlus properties = new PropertiesPlus();
		properties.setProperty("verbosity = 0");
		properties.setProperty("gridConstructionMode = scratch");
		properties.setProperty("nTessellations = 3");
		properties.setProperty("baseEdgeLengths = 64 32 16");
		if (!Double.isNaN(lat))
			properties.setProperty(String.format("rotateGrid = %1.5f %1.5f", lat, lon));

		GeoTessGrid grid = (GeoTessGrid) GeoTessBuilderMain.run(properties);

		// build a model
		GeoTessMetaData metaData = new GeoTessMetaData();
		metaData.setDescription("test");
		metaData.setLayerNames("CORE; MANTLE; CRUST");
		metaData.setLayerTessIds(new int[] { 0, 1, 2 });
		metaData.setAttributes("DATA", "furlongs/fortnight");
		metaData.setDataType(DataType.FLOAT);
		metaData.setModelSoftwareVersion(this.getClass().getCanonicalName());
		metaData.setModelGenerationDate(new Date().toString());

		GeoTessModel model = new GeoTessModel(grid, metaData);
		for (int layer = 0; layer < model.getNLayers(); ++layer) {
			float[] radii = new float[] { layer * 100, layer * 100 + 100 };
			for (int vtx = 0; vtx < model.getNVertices(); ++vtx)
				model.setProfile(vtx, layer, radii.clone());

			radii = new float[] { layer * 100, layer * 100 + 40, layer * 100 + 60, layer * 100 + 100 };
			int tessId = model.getMetaData().getTessellation(layer);
			HashSetInteger vertices = model.getGrid().getVertexIndicesTopLevel(tessId);
			Iterator it = vertices.iterator();
			while (it.hasNext()) {
				float[][] rawData = new float[radii.length][];
				for (int j = 0; j < rawData.length; ++j)
					rawData[j] = new float[] { 2F };

				model.setProfile(it.next(), layer, radii.clone(), rawData);
			}
		}

		model.setActiveRegion();

		return model;
	}

	public GeoTessModel getCrustalModel1Layer() throws Exception {

		// build a grid
		double centerLat = 20;
		double centerLon = 20;
		double polygonRadius = 20;
		double[] center = VectorGeo.getVectorDegrees(centerLat, centerLon);

		Polygon polygon = new Polygon(center, Math.toRadians(polygonRadius), 100);

		PropertiesPlus properties = new PropertiesPlus();
		properties.setProperty("verbosity = 0");
		properties.setProperty("gridConstructionMode = scratch");
		properties.setProperty("nTessellations = 1");
		properties.setProperty("baseEdgeLengths = 64");
		properties.setProperty(String.format("polygons = spherical_cap, %1.6f, %1.6f, %1.6f, 0, 16", centerLat,
				centerLon, polygonRadius));
		properties.setProperty(String.format("rotateGrid = %1.6f %1.6f", centerLat, centerLon));
		properties.setProperty("initialSolid = octahedron");

		GeoTessGrid grid = (GeoTessGrid) GeoTessBuilderMain.run(properties);

		// build a model
		GeoTessMetaData metaData = new GeoTessMetaData();
		metaData.setDescription("test");
		metaData.setLayerNames("CRUST");
		metaData.setAttributes("DATA", "furlongs/fortnight");
		metaData.setDataType(DataType.FLOAT);
		metaData.setModelSoftwareVersion(this.getClass().getCanonicalName());
		metaData.setModelGenerationDate(new Date().toString());

		GeoTessModel model = new GeoTessModel(grid, metaData);

		float[] radii = new float[] { 6371F - 30F, 6371F };
		for (int vtx = 0; vtx < model.getNVertices(); ++vtx)
			if (polygon.contains(grid.getVertex(vtx)))
				model.setProfile(vtx, 0, radii, new float[][] { { 2F } });
			else
				model.setProfile(vtx, 0, radii);

		model.setActiveRegion(polygon);

		return model;
	}

	public GeoTessModel getCrustalModel3Layers() throws Exception {

		PropertiesPlus properties = new PropertiesPlus();
		properties.setProperty("verbosity = 0");
		properties.setProperty("gridConstructionMode = scratch");
		properties.setProperty("nTessellations = 1");
		properties.setProperty("baseEdgeLengths = 16");

		GeoTessGrid grid = (GeoTessGrid) GeoTessBuilderMain.run(properties);

		// build a model
		GeoTessMetaData metaData = new GeoTessMetaData();
		metaData.setDescription("test");
		metaData.setLayerNames("MANTLE; LOWER_CRUST; UPPER_CRUST");
		metaData.setAttributes("VP", "furlongs/fortnight");
		metaData.setDataType(DataType.FLOAT);
		metaData.setModelSoftwareVersion(this.getClass().getCanonicalName());
		metaData.setModelGenerationDate(new Date().toString());

		GeoTessModel model = new GeoTessModel(grid, metaData);

		float[][] radii = new float[][] { new float[] { 6371F - 30F }, // mantle: PROFILE_THIN
				new float[] { 6371F - 30F, 6371F - 20F, 6371F - 10F }, // lower_crust: PROFILE_NPOINT
				new float[] { 6371F - 10F, 6371F } // upper_crust: PROFILE_CONSTANT
		};

		for (int vtx = 0; vtx < model.getNVertices(); ++vtx) {
			float[][][] data = new float[][][] { new float[][] { { 8F } },
					new float[][] { new float[] { 4F }, new float[] { 4F }, new float[] { 4F } },
					new float[][] { { 2F } } };

			for (int layer = 0; layer < 3; ++layer)
				model.setProfile(vtx, layer, radii[layer].clone(), data[layer]);
		}

		model.setActiveRegion();

		return model;
	}

	public GeoTessModel get2DModel() throws Exception {

		PropertiesPlus properties = new PropertiesPlus();
		properties.setProperty("verbosity = 0");
		properties.setProperty("gridConstructionMode = scratch");
		properties.setProperty("nTessellations = 1");
		properties.setProperty("baseEdgeLengths = 16");

		GeoTessGrid grid = (GeoTessGrid) GeoTessBuilderMain.run(properties);

		// build a model
		GeoTessMetaData metaData = new GeoTessMetaData();
		metaData.setDescription("test");
		metaData.setLayerNames("SURFACE");
		metaData.setAttributes("DATA", "furlongs/fortnight");
		metaData.setDataType(DataType.FLOAT);
		metaData.setModelSoftwareVersion(this.getClass().getCanonicalName());
		metaData.setModelGenerationDate(new Date().toString());

		GeoTessModel model = new GeoTessModel(grid, metaData);

		Polygon polygon = new Polygon(new double[] { 0, 0, 1 }, Math.toRadians(20), 100);

		for (int vtx = 0; vtx < model.getNVertices(); ++vtx)
			if (polygon.contains(grid.getVertex(vtx)))
				model.setProfile(vtx, Data.getDataFloat(new float[] { 2F }));
			else
				model.setProfile(vtx, Data.getDataFloat(new float[] { 0F }));

		model.setActiveRegion();

		return model;
	}

}
