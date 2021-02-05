package geotessbuildertest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

import gov.sandia.geotess.GeoTessGrid;
import gov.sandia.geotess.GeoTessModel;
import gov.sandia.geotess.GeoTessUtils;
import gov.sandia.geotessbuilder.GeoTessBuilderMain;
import gov.sandia.gmp.util.numerical.vector.EarthShape;
import gov.sandia.gmp.util.propertiesplus.PropertiesPlus;

public class GeoTessBuilderTest {

	@Test
	public void testGetSeismicityDepth() throws Exception {
		GeoTessModel seismicityDepthModel = GeoTessBuilderMain.getSeismicityDepthModel();
		assertEquals(3, seismicityDepthModel.getNAttributes());
	}

	@Test
	public void testPlatonicSolids() throws Exception {
		for (String solid : new String[] { "icosahedron", "octahedron", "tetrahedron", "tetrahexahedron" }) {
			PropertiesPlus properties = new PropertiesPlus();

			properties.setProperty("gridConstructionMode", "scratch");
			properties.setProperty("nTessellations", "2");
			properties.setProperty("baseEdgeLengths = 32, 16");
			properties.setProperty("initialSolid", solid);
			properties.setProperty("verbosity", 0);

			GeoTessGrid grid = (GeoTessGrid) GeoTessBuilderMain.run(properties);

			// System.out.println(grid.getPlatonicSolid());

			assertEquals(solid, grid.getPlatonicSolid().toString().toLowerCase());

		}

	}

	@Test
	public void testEulerRotation() throws Exception {
		double baseEdgeLengths = 16;

		PropertiesPlus properties = new PropertiesPlus();

		properties.setProperty("gridConstructionMode", "scratch");
		properties.setProperty("nTessellations", "1");
		properties.setProperty("baseEdgeLengths", baseEdgeLengths);
		properties.setProperty("verbosity", 0);

		// Second euler rotation angle needs to be the geocentric colatitude.

		double lat = -30, lon = 55;

		properties.setProperty(String.format("eulerRotationAngles = %1.8f %1.8f %1.8f", lon + 90,
				90 - EarthShape.WGS84.getGeocentricLatDegrees(lat), 0.));

		// System.out.println(properties.getProperty("eulerRotationAngles"));

		GeoTessGrid grid = (GeoTessGrid) GeoTessBuilderMain.run(properties);

		// System.out.println(GeoTessUtils.getLatLonString(grid.getVertex(0)));

		assertEquals(lat, GeoTessUtils.getLatDegrees(grid.getVertex(0)), 1e-6);
		assertEquals(lon, GeoTessUtils.getLonDegrees(grid.getVertex(0)), 1e-6);

	}

	@Test
	public void testSphericalCaps() throws Exception {
		double baseEdgeLengths = 4;

		PropertiesPlus properties = new PropertiesPlus();

		properties.setProperty("gridConstructionMode", "scratch");
		properties.setProperty("nTessellations", "1");
		properties.setProperty("baseEdgeLengths", baseEdgeLengths);
		properties.setProperty("verbosity", 0);

		double smallCircleRadius = 18;

		double lat = 10;
		double lon = 20;

		StringBuilder buf = new StringBuilder();
		buf.append(String.format("spherical_cap, %1.6f, %1.6f, %1.2f, 0, %1.4f; ", lat, lon, smallCircleRadius,
				baseEdgeLengths / 2));

		smallCircleRadius = 2;

		buf.append(String.format("spherical_cap, %1.6f, %1.6f, %1.2f, 0, %1.4f; ", lat, lon, smallCircleRadius,
				baseEdgeLengths / 4));

		properties.setProperty("polygons", buf.toString());

		GeoTessGrid grid = (GeoTessGrid) GeoTessBuilderMain.run(properties);

//		grid.writeGrid(new File("spherical_cap_test/junit_test_output/spherical_cap_grid.geotess"));
//		GeoTessModelUtils.vtkGrid(grid, "spherical_cap_test/junit_test_output/spherical_cap_test.vtk");	

		assertTrue(grid.getGridID().equals("A3F2F1E1B4F977FBA974F1C4D25C2CBD"));

	}

	@Test
	public void testTomo2d() throws Exception {
		File dir = new File("GeoTessBuilderExamples/tomo2dTest");

		PropertiesPlus properties = new PropertiesPlus(new File(dir, "gridbuilder.properties"));
		properties.setProperty("verbosity", 0);

		// System.out.println(properties);

		GeoTessBuilderMain.run(properties);

		GeoTessModel expectedModel = new GeoTessModel(new File(dir, "expected_model.geotess"));

		GeoTessModel actualModel = new GeoTessModel(new File(dir, "model.geotess"));

		assertEquals(expectedModel, actualModel);

		new File(dir, "model.geotess").delete();
		new File(dir, "model.vtk").delete();
		new File(dir, "continent_boundaries.vtk").delete();
	}

	@Test
	public void testThreeTessellations() throws Exception {
		File dir = new File("GeoTessBuilderExamples/threeTessTest");
		PropertiesPlus properties = new PropertiesPlus(new File(dir, "gridbuilder.properties"));
		properties.setProperty("verbosity", 0);

		// System.out.println(properties);

		GeoTessBuilderMain.run(properties);

		GeoTessGrid expectedGrid = new GeoTessGrid(new File(dir, "expected_grid.geotess"));

		GeoTessGrid actualGrid = new GeoTessGrid(new File(dir, "grid.geotess"));

		assertEquals(expectedGrid.getGridID(), actualGrid.getGridID());

		new File(dir, "grid.geotess").delete();
		new File(dir, "grid_0.vtk").delete();
		new File(dir, "grid_1.vtk").delete();
		new File(dir, "grid_2.vtk").delete();
		new File(dir, "continent_boundaries.vtk").delete();
	}

	@Test
	public void testPoints() throws Exception {
		File dir = new File("GeoTessBuilderExamples/pointTest");

		PropertiesPlus properties = new PropertiesPlus(new File(dir, "gridbuilder.properties"));
		properties.setProperty("verbosity", 0);

		// System.out.println(properties);

		GeoTessBuilderMain.run(properties);

		GeoTessGrid expectedGrid = new GeoTessGrid(new File(dir, "expected_grid.geotess"));

		GeoTessGrid actualGrid = new GeoTessGrid(new File(dir, "grid.geotess"));

		assertEquals(expectedGrid.getGridID(), actualGrid.getGridID());

		new File(dir, "grid.geotess").delete();
		new File(dir, "grid.vtk").delete();
		new File(dir, "continent_boundaries.vtk").delete();
	}

	@Test
	public void testPaths() throws Exception {
		File dir = new File("GeoTessBuilderExamples/pathTest");

		PropertiesPlus properties = new PropertiesPlus(new File(dir, "gridbuilder.properties"));
		properties.setProperty("verbosity", 0);

		// System.out.println(properties);

		GeoTessBuilderMain.run(properties);

		GeoTessGrid expectedGrid = new GeoTessGrid(new File(dir, "expected_grid.geotess"));

		GeoTessGrid actualGrid = new GeoTessGrid(new File(dir, "grid.geotess"));

		assertEquals(expectedGrid.getGridID(), actualGrid.getGridID());

		new File(dir, "grid.geotess").delete();
		new File(dir, "grid.vtk").delete();
		new File(dir, "continent_boundaries.vtk").delete();
	}

	@Test
	public void testPolygons() throws Exception {
		File dir = new File("GeoTessBuilderExamples/polygonTest");

		PropertiesPlus properties = new PropertiesPlus(new File(dir, "gridbuilder.properties"));
		properties.setProperty("verbosity", 0);

		// System.out.println(properties);

		GeoTessBuilderMain.run(properties);

		GeoTessGrid expectedGrid = new GeoTessGrid(new File(dir, "expected_grid.geotess"));

		GeoTessGrid actualGrid = new GeoTessGrid(new File(dir, "grid.geotess"));

		assertEquals(expectedGrid.getGridID(), actualGrid.getGridID());

		new File(dir, "grid.geotess").delete();
		new File(dir, "grid.vtk").delete();
		new File(dir, "continent_boundaries.vtk").delete();
	}

	@Test
	public void testMultiplePolygons() throws Exception {
		PropertiesPlus properties = new PropertiesPlus();
		properties.setProperty("verbosity", 0);
		properties.setProperty("gridConstructionMode = scratch");
		properties.setProperty("nTessellations = 1");
		properties.setProperty("baseEdgeLengths = 8");
		properties.setProperty("polygons = testdata/test_polygons.kml, 0, 1");
		properties.setProperty("outputModelFile = testdata/grid.geotess");
		properties.setProperty("vtkFile = testdata/grid.vtk");

		GeoTessBuilderMain.run(properties);

		GeoTessGrid expectedGrid = new GeoTessGrid(new File("testdata/expected_grid.geotess"));

		GeoTessGrid actualGrid = new GeoTessGrid(new File("testdata/grid.geotess"));

		assertEquals(expectedGrid.getGridID(), actualGrid.getGridID());

		new File("testdata/grid.geotess").delete();
		new File("testdata/grid.vtk").delete();
		new File("testdata/continent_boundaries.vtk").delete();
	}

}
