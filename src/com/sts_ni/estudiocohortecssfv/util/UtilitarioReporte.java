package com.sts_ni.estudiocohortecssfv.util;

import java.awt.print.PrinterException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.Attribute;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.PrintServiceAttributeSet;
import javax.print.attribute.standard.Destination;
import javax.print.attribute.standard.PrinterInfo;
import javax.print.attribute.standard.PrinterIsAcceptingJobs;
import javax.print.attribute.standard.PrinterLocation;
import javax.print.attribute.standard.PrinterMakeAndModel;
import javax.print.attribute.standard.PrinterState;

import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import ni.com.sts.estudioCohorteCSSFV.modelo.HojaConsulta;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;



/**
 * Clase utilitaria para la generaci�n de los reportes en un Viewer.
 * <p>
 * @author Honorio Trejos S.
 * @author <a href=mailto:honorio.trejos@simmo.cl>honorio.trejos@simmo.cl</a>
 * @version 1.0, &nbsp; 11/07/2013
 * @since
 */
public class UtilitarioReporte  {

 private static CompositeConfiguration config;
 
 static Logger logger;
 
 static {
	 config= UtilProperty.getConfiguration("EstudioCohorteCssfvMovilWSExt.properties", "com/sts_ni/estudiocohortecssfv/properties/EstudioCohorteCssfvMovilWSInt.properties");
	 logger = Logger.getLogger("UtilitarioReporte");
 }
    
    /**
     * M�todo utilitario para generar reporte.
     *
     * @param titulo         Titulo del visor de reporte.
     * @param source         Ruta del reporte compilado (.jasper).
     * @param parametros     Lista de par�metros para el reporte.
     * @param collectionDataSource Colecci�n de DataSources para el reporte.
     */
     public static byte[] mostrarReporte( String nombreReporte,
             Map<String, Object> parametros, List<?> collectionDataSource, boolean multipleReporte, HojaConsulta datosAdicionales) {

        return getMostrarReporte( nombreReporte, parametros, collectionDataSource, false,multipleReporte, datosAdicionales);
     }

    /**
    * M�todo utilitario para generar reporte.
    *
    * @param titulo         Titulo del visor de reporte.
    * @param source         Ruta del reporte compilado (.jasper)
    * @param parametros     Lista de par�metros para el reporte.
    * @param collectionDataSource Colecci�n de DataSources para el reporte
    * @param indicarDirSubReport  Indica si se le pasar� al sub-reporte el path
    *                             donde se encuentra el reporte principal.
    */
    public static byte[] getMostrarReporte( String nombreReporte,
            Map<String, Object> parametros, List<?> collectionDataSource,
            boolean indicarDirSubReport, boolean multipleReporte, HojaConsulta datosAdicionales) {
        
    	try{
    		
    		 // Construir la URL donde se encuentran los reportes.
            String path = System.getProperty("jboss.server.data.dir") + System.getProperty("file.separator").charAt(0) + config.getString("ruta.reporte") + (nombreReporte.contains(".jasper")?nombreReporte:nombreReporte + ".jasper");
            String pathPag2 = System.getProperty("jboss.server.data.dir") + System.getProperty("file.separator").charAt(0) + config.getString("ruta.reporte") + ( (nombreReporte+"2").contains(".jasper")?(nombreReporte+"2"):(nombreReporte+"2") + ".jasper");
                        
            path = path.replace('/', System.getProperty("file.separator").charAt(0));

                        
            logger.debug("path : " + path);
            logger.debug("================ URL ================");
            
            // Indicar si se le pasar� al subreporte la URL donde se encuentra el reporte principal.
            if (indicarDirSubReport) {       
                String pathSubReport = System.getProperty("user.dir") + System.getProperty("file.separator").charAt(0) +  config.getString("ruta.reportes");
                pathSubReport = pathSubReport.replace('/', System.getProperty("file.separator").charAt(0));
                parametros.put("SUBREPORT_DIR", pathSubReport); // Par�metro estandar de JasperReport.
                
            }
            
            JasperPrint report =JasperFillManager.fillReport(path, parametros, new JRBeanCollectionDataSource(collectionDataSource));
            if (multipleReporte){
	            JasperPrint fileAnexo = JasperFillManager.fillReport(pathPag2, parametros, new JRBeanCollectionDataSource(collectionDataSource));
	            report.addPage(fileAnexo.removePage( 0 )) ;
	            
	            if(datosAdicionales != null){
	            	HashMap params = new HashMap(); 
	            	params.put("planes", datosAdicionales.getPlanes());
	            	params.put("historiaExamenFisico", datosAdicionales.getHistoriaExamenFisico());
	            	params.put("numHojaConsulta", datosAdicionales.getNumHojaConsulta());
	            	params.put("codExpediente", datosAdicionales.getCodExpediente());
	            	params.put("expedienteFisico", datosAdicionales.getExpedienteFisico());
	            	
	            	String pathPag3 = System.getProperty("jboss.server.data.dir") + System.getProperty("file.separator").charAt(0) + config.getString("ruta.reporte") + ( (nombreReporte+"3").contains(".jasper")?(nombreReporte+"3"):(nombreReporte+"3") + ".jasper");
	            	JasperPrint fileAnexo2 = JasperFillManager.fillReport(pathPag3, params, new JRBeanCollectionDataSource(collectionDataSource));
	            	report.addPage(fileAnexo2.removePage( 0 )) ;
	            }
            }
          return  JasperExportManager.exportReportToPdf(report);
    	}
    	catch(Exception e){
    		e.printStackTrace();
    	}
    	return null;
    }
    
    public  void imprimirDocumento(String nombres,byte[] archivoByte){
    	
    	String path = System.getProperty("jboss.server.data.dir") + System.getProperty("file.separator").charAt(0) + config.getString("ruta.pdf") + (nombres.contains(".pdf")?nombres:nombres + ".pdf");
    	
  	    File file = new File(path);
    	try
    	{
              //convert array of bytes into file
           FileOutputStream fileOuputStream =
                      new FileOutputStream(file);
           fileOuputStream.write(archivoByte);
           fileOuputStream.close();
           
           PDDocument pdf = PDDocument.load(path);
		   pdf.silentPrint();
    		
    	} catch(IOException e) {
    		
    		e.printStackTrace();
    	} catch (PrinterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	        
	}
    
    
    


public  void imprimirDocumentoTest(String nombres){
    	
    	//String path = System.getProperty("jboss.server.data.dir") + System.getProperty("file.separator").charAt(0) + config.getString("ruta.pdf") + (nombres.contains(".docx")?nombres:nombres + ".docx");
    	String path = "c:\\HojaConsulta.docx";
    	try
    	{
           System.out.println("----------------->" + path);
    		
    		FileInputStream inputStream = null;
            inputStream = new FileInputStream(path);
            if (inputStream == null) {
                return;
            }
            
           DocFlavor docFormat = DocFlavor.INPUT_STREAM.AUTOSENSE;
           Doc document = new SimpleDoc(inputStream, docFormat, null);
    
         //  printAvailable();
           PrintRequestAttributeSet attributeSet = new HashPrintRequestAttributeSet();
           PrintService defaultPrintService = PrintServiceLookup.lookupDefaultPrintService();
    
           if (defaultPrintService != null) {
               DocPrintJob printJob = defaultPrintService.createPrintJob();
               try {
                   printJob.print(document, attributeSet);
    
               } catch (Exception e) {
                   e.printStackTrace();
               }
           } else {
               System.err.println("No existen impresoras instaladas");
           }
           
           inputStream.close();
    		
    	}
    	catch(IOException e)
    	
    	{
    		
    		e.printStackTrace();
    	}
	        
	}

	public static void printAvailable() {
		 
	    // busca los servicios de impresion...
	    PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
	
	    // -- ver los atributos de las impresoras...
	    for (PrintService printService : services) {
	
	        System.out.println(" ---- IMPRESORA: " + printService.getName());
	
	        PrintServiceAttributeSet printServiceAttributeSet = printService.getAttributes();
	
	        System.out.println("--- atributos");
	
	        // todos los atributos de la impresora
	        Attribute[] a = printServiceAttributeSet.toArray();
	        for (Attribute unAtribute : a) {
	            System.out.println("atributo: " + unAtribute.getName());
	        }
	
	        System.out.println("--- viendo valores especificos de los atributos ");
	
	        // valor especifico de un determinado atributo de la impresora
	        System.out.println("PrinterLocation: " + printServiceAttributeSet.get(PrinterLocation.class));
	        System.out.println("PrinterInfo: " + printServiceAttributeSet.get(PrinterInfo.class));
	        System.out.println("PrinterState: " + printServiceAttributeSet.get(PrinterState.class));
	        System.out.println("Destination: " + printServiceAttributeSet.get(Destination.class));
	        System.out.println("PrinterMakeAndModel: " + printServiceAttributeSet.get(PrinterMakeAndModel.class));
	        System.out.println("PrinterIsAcceptingJobs: " + printServiceAttributeSet.get(PrinterIsAcceptingJobs.class));
	
	    }
	
	}

    /**
     * M�todo para imprimir directamente en la impresora.
     *
     * @param nombreReporte
     * @param parametros
     * @param collectionDataSource
     */
//    public static void enviarAImpresora(String nombreReporte,
//            Map<String, Object> parametros, List<?> collectionDataSource) {
//
//        Window win = (Window) Executions.createComponents("etiqueta.zul", null, null);
//
//        //barCode = (String) parametros.get("pCodigoBulto");
//
//
//        Jasperreport report = null;
//        report = (Jasperreport) win.getFellow("imprimirReporte");
//
//        String path = System.getProperty("user.dir") + System.getProperty("file.separator").charAt(0) + config.getString("ruta.reportes") + (nombreReporte.contains(".jasper")?nombreReporte:nombreReporte + ".jasper");
//        path = path.replace('/', System.getProperty("file.separator").charAt(0));
//
//        report.setSrc(path);
//        report.setType("html");
//        report.setParameters(parametros!=null ? parametros : null);
//        report.setDatasource(collectionDataSource!=null ? new JRBeanCollectionDataSource(collectionDataSource) : null);
//
//        Clients.evalJavaScript("printing()");
//
//    }


    // ------------------------------ getters y setters ------------------------------

   

}
