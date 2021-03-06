package com.sts_ni.estudiocohortecssfv.datos.inicio;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ni.com.sts.estudioCohorteCSSFV.modelo.EstudioCatalogo;
import ni.com.sts.estudioCohorteCSSFV.modelo.HojaConsulta;
import ni.com.sts.estudioCohorteCSSFV.modelo.HojaInfluenza;
import ni.com.sts.estudioCohorteCSSFV.modelo.HojaZika;
import ni.com.sts.estudioCohorteCSSFV.modelo.SeguimientoInfluenza;
import ni.com.sts.estudioCohorteCSSFV.modelo.SeguimientoZika;

import org.hibernate.Query;
import org.hibernate.transform.Transformers;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.sts_ni.estudiocohortecssfv.dto.SeguimientoInfluenzaReporte;
import com.sts_ni.estudiocohortecssfv.dto.SeguimientoZikaReporte;
import com.sts_ni.estudiocohortecssfv.servicios.ExpedienteService;
import com.sts_ni.estudiocohortecssfv.servicios.HojaConsultaReporteService;
import com.sts_ni.estudiocohortecssfv.util.HibernateResource;
import com.sts_ni.estudiocohortecssfv.util.Mensajes;
import com.sts_ni.estudiocohortecssfv.util.UtilResultado;
import com.sts_ni.estudiocohortecssfv.util.UtilitarioReporte;

/***
 * Clase que maneja la conexion a los Datos, para ejecutar los procesos relacionados a
 * Expediente, Seguimiento Influenza y Seguimiento Zika.
 *
 */
public class ExpedienteDA implements ExpedienteService {

	private static final HibernateResource HIBERNATE_RESOURCE = new HibernateResource();
	
	private HojaConsultaReporteService consultaReporteService;
	
	private static String QUERY_HOJA_CONSULTA_BY_ID = "select h from HojaConsulta h where h.secHojaConsulta = :id";


	/***
	 * Metodo que obtiene Hoja consulta pro codigo expediente.
	 * @param codExpediente, Codigo Expediente.
	 */
	@Override
	public String getListaHojaConsultaExp(int codExpediente) {
		String result = null;
		try {

			List oLista = new LinkedList();
			Map fila = null;

			String sql = "select  " + " h.numHojaConsulta, "
					+ " to_char(h.fechaCierre, 'DD-MON-YY'), "
					+ " to_char(h.fechaCierre, 'HH:MI:SS AM'), "
					+ " e.descripcion, " + 
					" (select um.nombre from UsuariosView um where h.usuarioMedico = um.id),  " +
					" h.secHojaConsulta "
					+ " from HojaConsulta h, EstadosHoja e"
					+ " where h.estado = e.codigo ";

			sql += " and h.codExpediente=:codExpediente ";

			sql += "order by h.ordenLlegada asc";

			Query query = HIBERNATE_RESOURCE.getSession().createQuery(sql);

			if (codExpediente > 0)
				query.setParameter("codExpediente", codExpediente);

			List<Object[]> objLista = (List<Object[]>) query.list();

			if (objLista != null && objLista.size() > 0) {

				for (Object[] object : objLista) {

					// Construir la fila del registro actual usando arreglos

					fila = new HashMap();
					fila.put("numHojaConsulta", object[0]);
					if (object[1] != null) {
						fila.put("fechaCierre", object[1].toString());
						fila.put("horaCierre", object[2].toString());
					} else {
						fila.put("fechaCierre", "--");
						fila.put("horaCierre", "--");
					}
					fila.put("estado", object[3].toString());
					fila.put("medicoCierre", (object[4] != null) ? object[4] : "--");
					fila.put("secHojaConsulta", object[5]);

					oLista.add(fila);

				}

				// Construir la lista a una estructura JSON
				result = UtilResultado.parserResultado(oLista, "",
						UtilResultado.OK);
			} else {
				result = UtilResultado.parserResultado(null, Mensajes.NO_DATOS,
						UtilResultado.INFO);
			}

		} catch (Exception e) {
			e.printStackTrace();
			result = UtilResultado.parserResultado(null,
					Mensajes.ERROR_NO_CONTROLADO, UtilResultado.ERROR);
		} finally {
			if (HIBERNATE_RESOURCE.getSession().isOpen()) {
				HIBERNATE_RESOURCE.close();
			}
		}
		return result;
	}
	
	/***
	 * Metodo para buscar los datos del paciente por codigo expediente.
	 * @param codExpediente, Codigo Expediente.
	 */
	@Override
	public String buscarPacienteCrearHoja(int codExpediente) {
		String result = null;
		try {
			List oLista = new LinkedList();
			Map fila = null;

			String sql = "select p.nombre1, p.nombre2, " + 
					" p.apellido1, p.apellido2 " + 
					" from Paciente p, ConsEstudios ce " + 
					" where p.codExpediente = :codExpediente " + 
					" and p.codExpediente = ce.codigoExpediente " +
					" and ce.retirado != '1' ";

			Query query = HIBERNATE_RESOURCE.getSession().createQuery(sql);
			query.setParameter("codExpediente", codExpediente);
			query.setMaxResults(1);

			Object[] paciente = (Object[]) query.uniqueResult();

			if (paciente != null && paciente.length > 0) {
				
				sql = "select ec " + 
					" from ConsEstudios c, EstudioCatalogo ec " + 
					" where c.codigoConsentimiento = ec.codEstudio" + 
					" and c.codigoExpediente = :codExpediente " + 
					" and c.retirado != '1' " +
					" group by ec.codEstudio, ec.descEstudio";

				query = HIBERNATE_RESOURCE.getSession().createQuery(sql);

				query.setParameter("codExpediente", codExpediente);

				List<EstudioCatalogo> lstConsEstudios = (List<EstudioCatalogo>) query.list();
				StringBuffer codigosEstudios = new StringBuffer();

				for (EstudioCatalogo estudioCatalogo : lstConsEstudios) {
					codigosEstudios.append(estudioCatalogo.getDescEstudio()).append(",");
				}
				
				fila = new HashMap();

				fila.put("nomPaciente", paciente[0].toString() + " " + 
				((paciente[1] != null) ? paciente[1].toString(): "") + " " + 
				paciente[2].toString() + " " + 
				((paciente[3] != null) ? paciente[3].toString() : ""));
				
				fila.put("estudioPaciente", codigosEstudios != null && 
						!codigosEstudios.toString().isEmpty() ? 
								(codigosEstudios.substring(0, (codigosEstudios.length() - 1))): "");

				oLista.add(fila);

				// Construir la lista a una estructura JSON
				result = UtilResultado.parserResultado(oLista, "", UtilResultado.OK);
				
			} else {
				result = UtilResultado.parserResultado(null,
						Mensajes.CODIGO_PACIENTE_NO_EXISTE, UtilResultado.INFO);
			}

		} catch (Exception e) {
			e.printStackTrace();
			result = UtilResultado.parserResultado(null,
					Mensajes.ERROR_NO_CONTROLADO, UtilResultado.ERROR);
		} finally {
			if (HIBERNATE_RESOURCE.getSession().isOpen()) {
				HIBERNATE_RESOURCE.close();
			}
		}
		return result;
	}

	/***
	 * Funcion para buscar el seguimiento influenza por numero de hoja de seguimiento.
	 * @param numHojaSeguimiento, Numero de seguimiento.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public String buscarHojaSeguimientoInfluenza(int numHojaSeguimiento) {
		String result = null;
		try {
			List oLista = new LinkedList();
			Map fila = null;

			String sql = "select p.nombre1, p.nombre2, " + 
					" p.apellido1, p.apellido2, p.codExpediente, hs.numHojaSeguimiento, " +
					" hs.fif, hs.fis, " + 
					" hs.secHojaInfluenza, hs.cerrado " + 
					" from Paciente p, HojaInfluenza hs, ConsEstudios ce " + 
					" where hs.numHojaSeguimiento = :numHojaSeguimiento " + 
					" and p.codExpediente = hs.codExpediente " +
					" and p.codExpediente = ce.codigoExpediente " +
					" and ce.retirado != '1' order by hs.secHojaInfluenza desc";

			Query query = HIBERNATE_RESOURCE.getSession().createQuery(sql);
			query.setParameter("numHojaSeguimiento", numHojaSeguimiento);
			query.setMaxResults(1);

			Object[] paciente = (Object[]) query.uniqueResult();

			if (paciente != null && paciente.length > 0) {
				
				sql = "select ec " + 
						" from ConsEstudios c, EstudioCatalogo ec " + 
						" where c.codigoConsentimiento = ec.codEstudio" + 
						" and c.codigoExpediente = :codExpediente " + 
						" and c.retirado != '1'" +
						" group by ec.codEstudio, ec.descEstudio";

				query = HIBERNATE_RESOURCE.getSession().createQuery(sql);

				query.setParameter("codExpediente", ((Integer) paciente[4]).intValue());

				List<EstudioCatalogo> lstConsEstudios = (List<EstudioCatalogo>) query.list();
				StringBuffer codigosEstudios = new StringBuffer();

				for (EstudioCatalogo estudioCatalogo : lstConsEstudios) {
					codigosEstudios.append(estudioCatalogo.getDescEstudio()).append(",");
				}

				fila = new HashMap();
				fila.put("nomPaciente", paciente[0].toString() + 
						" " + ((paciente[1] != null) ? paciente[1].toString() : "") + 
						" " + paciente[2].toString() + " " + 
						((paciente[3] != null) ? paciente[3].toString() : ""));
				fila.put("estudioPaciente", (codigosEstudios != null && 
						!codigosEstudios.toString().isEmpty()) ? 
								(codigosEstudios.substring(0, (codigosEstudios.length() - 1))): "");
				fila.put("codExpediente", ((Integer)paciente[4]).intValue());
				fila.put("numHojaSeguimiento", ((Integer)paciente[5]).intValue());
				fila.put("fif", (paciente[6] != null) ? paciente[6].toString() : "");
				fila.put("fis", (paciente[7] != null) ? paciente[7].toString() : "");
				fila.put("secHojaInfluenza", ((Integer)paciente[8]).intValue());
				fila.put("cerrado", paciente[9].toString().charAt(0));

				oLista.add(fila);

				// Construir la lista a una estructura JSON
				result = UtilResultado.parserResultado(oLista, "", UtilResultado.OK);
			} else {
				result = UtilResultado.parserResultado(null,
						Mensajes.REGISTRO_NO_ENCONTRADO, UtilResultado.INFO);
			}

		} catch (Exception e) {
			e.printStackTrace();
			result = UtilResultado.parserResultado(null,
					Mensajes.ERROR_NO_CONTROLADO, UtilResultado.ERROR);
		} finally {
			if (HIBERNATE_RESOURCE.getSession().isOpen()) {
				HIBERNATE_RESOURCE.close();
			}
		}
		return result;
	}

	/***
	 * Funcion para buscar paciente con hoja de seguimiento influenza por codigo de expediente.
	 * @param codExpediente, Codigo Expediente.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public String buscarPacienteSeguimientoInfluenza(int codExpediente) {
		String result = null;
		try {
			List oLista = new LinkedList();
			Map fila = null;

			String sql = "select p.nombre1, p.nombre2, " + 
					" p.apellido1, p.apellido2, p.codExpediente, hs.numHojaSeguimiento, " + 
					" hs.fif, hs.fis, to_char(hs.fechaInicio, 'yyyyMMdd'), to_char(hs.fechaCierre, 'yyyyMMdd'), " + 
					" hs.secHojaInfluenza, hs.cerrado " + 
					" from Paciente p, HojaInfluenza hs, ConsEstudios ce " + 
					" where p.codExpediente = :codExpediente " + 
					" and p.codExpediente = hs.codExpediente " +
					" and p.codExpediente = ce.codigoExpediente " +
					" and ce.retirado != '1' order by hs.secHojaInfluenza desc";

			Query query = HIBERNATE_RESOURCE.getSession().createQuery(sql);
			query.setParameter("codExpediente", codExpediente);
			query.setMaxResults(1);

			Object[] paciente = (Object[]) query.uniqueResult();

			if (paciente != null && paciente.length > 0) {
				
				sql = "select ec " + 
						" from ConsEstudios c, EstudioCatalogo ec " + 
						" where c.codigoConsentimiento = ec.codEstudio" + 
						" and c.codigoExpediente = :codExpediente " + 
						" and c.retirado != '1'" +
						" group by ec.codEstudio, ec.descEstudio";

				query = HIBERNATE_RESOURCE.getSession().createQuery(sql);

				query.setParameter("codExpediente", codExpediente);

				List<EstudioCatalogo> lstConsEstudios = (List<EstudioCatalogo>) query.list();
				StringBuffer codigosEstudios = new StringBuffer();

				for (EstudioCatalogo estudioCatalogo : lstConsEstudios) {
					codigosEstudios.append(estudioCatalogo.getDescEstudio()).append(",");
				}

				fila = new HashMap();
				// fila.put("nombrePaciente", paciente[0].toString()+ " " +
				// paciente[1].toString()+ " " + paciente[2].toString()+ " "
				// + paciente[3].toString());

				fila.put("nomPaciente", paciente[0].toString() + 
						" " + 
						((paciente[1] != null) ? paciente[1].toString() : "") + 
						" " + paciente[2].toString() + " " + 
						((paciente[3] != null) ? paciente[3].toString() : ""));
				
				fila.put("estudioPaciente", codigosEstudios != null && 
						!codigosEstudios.toString().isEmpty() ? 
								(codigosEstudios.substring(0, (codigosEstudios.length() - 1))): "");

				fila.put("codExpediente", ((Integer)paciente[4]).intValue());

				if (paciente.length > 5) {
					fila.put("numHojaSeguimiento", ((Integer)paciente[5]).intValue());
					fila.put("fif", (paciente[6] != null) ? paciente[6].toString() : "");
					fila.put("fis", (paciente[7] != null) ? paciente[7].toString() : "");
					fila.put("secHojaInfluenza", ((Integer)paciente[10]).intValue());
					fila.put("cerrado", paciente[11].toString().charAt(0));
				}

				oLista.add(fila);

				// Construir la lista a una estructura JSON
				result = UtilResultado.parserResultado(oLista, "",
						UtilResultado.OK);
			} else {
				result = UtilResultado.parserResultado(null,
						Mensajes.REGISTRO_NO_ENCONTRADO, UtilResultado.INFO);
			}

		} catch (Exception e) {
			e.printStackTrace();
			result = UtilResultado.parserResultado(null,
					Mensajes.ERROR_NO_CONTROLADO, UtilResultado.ERROR);
		} finally {
			if (HIBERNATE_RESOURCE.getSession().isOpen()) {
				HIBERNATE_RESOURCE.close();
			}
		}
		return result;
	}
	
	/***
	 * Metodo para crear un nuevo seguimiento influenza.
	 * @param paramCrearHoja, JSON con los parametros requeridos para crear seguimiento.
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public String crearSeguimientoInfluenza(String paramCrearHoja) {
		String result = null;
		try {

			int codExpediente;
			String sql;
			Query query;
			HojaInfluenza hojaInfluenza;
			SeguimientoInfluenza seguimientoInfluenza;

			JSONParser parser = new JSONParser();
			Object obj = (Object) parser.parse(paramCrearHoja);
			JSONObject crearHojaJson = (JSONObject) obj;

			codExpediente = (((Number) crearHojaJson.get("codExpediente"))
					.intValue());
			
			//obtenemos la ultima hoja de consulta para el c�digo de expediente
			sql = "select h from HojaConsulta h " +
				 " where h.codExpediente = :codExpediente order by h.secHojaConsulta desc ";

			query = HIBERNATE_RESOURCE.getSession().createQuery(sql);
			query.setParameter("codExpediente", codExpediente);
			query.setMaxResults(1);
			
			HojaConsulta hojaConsulta = (HojaConsulta) query.uniqueResult();
			
			if(hojaConsulta == null){
				result = UtilResultado.parserResultado(null, Mensajes.NO_EXISTE_HC_CODEXP, UtilResultado.INFO);
			
			}else{
				
				if(hojaConsulta.getFif() == null || hojaConsulta.getFis() == null){
					return UtilResultado.parserResultado(null, Mensajes.HOJA_SIN_FIS_FIF, UtilResultado.INFO);
				}
				
				//verificando si tiene hojas abiertas
				sql = "select count(*) from hoja_influenza where cerrado = 'N' and cod_expediente = :codExpediente";
				query = HIBERNATE_RESOURCE.getSession().createSQLQuery(sql);
				query.setParameter("codExpediente", codExpediente);
				
				BigInteger totalActivos = (BigInteger) query.uniqueResult();
				
				//Si tiene uno o mas activos retornamos aviso
				if(totalActivos.intValue() > 0){
					return UtilResultado.parserResultado(null, Mensajes.HOJA_INF_NO_CERRADA, UtilResultado.INFO);
				}				
				
				String FIF, FIS;
				SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
				FIF = hojaConsulta.getFif() != null ? sdf.format(hojaConsulta.getFif()) : "";
				FIS = hojaConsulta.getFis() != null ? sdf.format(hojaConsulta.getFis()) : "";
			
				sql = "select max(h.numHojaSeguimiento) "
						+ " from HojaInfluenza h ";
	
				query = HIBERNATE_RESOURCE.getSession().createQuery(sql);
	
				Integer maxNumHojaSeguimiento = (query.uniqueResult() == null) ? 1 : 
					((Integer) query.uniqueResult()).intValue() + 1;
				
				Calendar fechaInicio = Calendar.getInstance();
				Object objFechaInicio = (Object) parser.parse(crearHojaJson.get("fechaInicio").toString());
				JSONObject fechaInicioJson = (JSONObject) objFechaInicio;
				
				fechaInicio.set(((Number)fechaInicioJson.get("year")).intValue(), 
						((Number)fechaInicioJson.get("month")).intValue(), 
						((Number)fechaInicioJson.get("dayOfMonth")).intValue());
	
				hojaInfluenza = new HojaInfluenza();
				hojaInfluenza.setNumHojaSeguimiento(maxNumHojaSeguimiento);
				hojaInfluenza.setCodExpediente(codExpediente);
				hojaInfluenza.setFechaInicio(fechaInicio.getTime());
				hojaInfluenza.setCerrado('N');				
				hojaInfluenza.setFif(FIF);
				hojaInfluenza.setFis(FIS);
				
				HIBERNATE_RESOURCE.begin();
				HIBERNATE_RESOURCE.getSession().saveOrUpdate(hojaInfluenza);
				HIBERNATE_RESOURCE.commit();
				
				List oLista = new LinkedList();
				Map fila = null;
				fila = new HashMap();
				fila.put("numHojaSeguimiento", hojaInfluenza.getNumHojaSeguimiento());
				fila.put("codExpediente", hojaInfluenza.getCodExpediente());
				fila.put("fif", FIF);
				fila.put("fis", FIS);
				oLista.add(fila);
				result = UtilResultado.parserResultado(oLista, "", UtilResultado.OK);
			}
		} catch (Exception e) {
			e.printStackTrace();
			result = UtilResultado.parserResultado(null, Mensajes.ERROR_NO_CONTROLADO + e.getMessage(),
					UtilResultado.ERROR);
			HIBERNATE_RESOURCE.rollback();
			// TODO: handle exception
		} finally {
			if (HIBERNATE_RESOURCE.getSession().isOpen()) {
				HIBERNATE_RESOURCE.close();
			}
		}
		return result;
	}

	/***
	 * Metodo para guardar la cabezera y detalle de seguimiento influenza.
	 * @param, JSON Datos de la cabecera y detalle.
	 */
	@Override
	public String guardarSeguimientoInfluenza(String paramHojaInfluenza,
			String paramSeguimientoInfluenza) {
		String result = null;
		try {

			int codExpediente;
			int numHojaSeguimiento;
			String sql;
			Query query;
			HojaInfluenza hojaInfluenza = new HojaInfluenza();
			SeguimientoInfluenza seguimientoInfluenza;
			SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");

			JSONParser parser = new JSONParser();
			Object obj = (Object) parser.parse(paramHojaInfluenza);
			JSONObject hojaInfluenzaJSON = (JSONObject) obj;

			obj = new Object();
			obj = parser.parse(paramSeguimientoInfluenza);
			JSONArray seguimientoInfluenzaArray = (JSONArray) obj;

			codExpediente = (((Number) hojaInfluenzaJSON.get("codExpediente"))
					.intValue());
			numHojaSeguimiento = ((Number) hojaInfluenzaJSON
					.get("numHojaSeguimiento")).intValue();
					      
			if (numHojaSeguimiento == 0) {
//				sql = "select max(h.numHojaSeguimiento) "
//						+ " from HojaInfluenza h ";
//
//				query = HIBERNATE_RESOURCE.getSession().createQuery(sql);
//
//				Integer maxNumHojaSeguimiento = (query.uniqueResult() == null) ? 1
//						: ((Integer) query.uniqueResult()).intValue() + 1;
//
//				hojaInfluenza = new HojaInfluenza();
//				hojaInfluenza.setNumHojaSeguimiento(maxNumHojaSeguimiento);
//				hojaInfluenza.setCodExpediente(codExpediente);
//				hojaInfluenza.setFis(hojaInfluenzaJSON.get("fis").toString());
//				hojaInfluenza.setFif(hojaInfluenzaJSON.get("fif").toString());
//				if (hojaInfluenzaJSON.get("fechaCierre") != null)
//					hojaInfluenza.setFechaCierre(df.parse(hojaInfluenzaJSON
//							.get("fechaCierre").toString()));
//				hojaInfluenza.setCerrado(hojaInfluenzaJSON.get("cerrado")
//						.toString().charAt(0));
			} else {
				sql = "select h from HojaInfluenza h " +
						" where h.codExpediente = :codExpediente " +
						" and h.numHojaSeguimiento = :numHojaSeguimiento";
				
				query = HIBERNATE_RESOURCE.getSession().createQuery(sql);
				query.setParameter("codExpediente", codExpediente);
				query.setParameter("numHojaSeguimiento", numHojaSeguimiento);

				hojaInfluenza = ((HojaInfluenza) query.uniqueResult());

				hojaInfluenza.setNumHojaSeguimiento(numHojaSeguimiento);
				hojaInfluenza.setCodExpediente(codExpediente);
				hojaInfluenza.setFis(hojaInfluenzaJSON.get("fis").toString());
				hojaInfluenza.setFif(hojaInfluenzaJSON.get("fif").toString());
				if (hojaInfluenzaJSON.containsKey("fechaCierre") && 
						hojaInfluenzaJSON.get("fechaCierre") != null) {
					hojaInfluenza.setFechaCierre(df.parse(hojaInfluenzaJSON
							.get("fechaCierre").toString()));
				}

			}
			
			if(hojaInfluenza.getCerrado() != 'S' && hojaInfluenza.getCerrado() != 's') {
				hojaInfluenza.setCerrado(hojaInfluenzaJSON.get("cerrado").toString().charAt(0));
				HIBERNATE_RESOURCE.begin();
				HIBERNATE_RESOURCE.getSession().saveOrUpdate(hojaInfluenza);
				HIBERNATE_RESOURCE.commit();
	
				if (paramSeguimientoInfluenza != "") {
	
					for (int i = 0; i < seguimientoInfluenzaArray.size(); i++) {
						seguimientoInfluenza = new SeguimientoInfluenza();
						obj = new Object();
						obj = (Object) parser.parse(seguimientoInfluenzaArray
								.get(i).toString());
						JSONObject seguimientoInfluenzaJSON = (JSONObject) obj;
	
						sql = "select s from SeguimientoInfluenza s where s.secHojaInfluenza = :secHojaInfluenza and s.controlDia = :controlDia";
						query = HIBERNATE_RESOURCE.getSession().createQuery(sql);
						query.setParameter("secHojaInfluenza",
								hojaInfluenza.getSecHojaInfluenza());
						query.setParameter("controlDia", Integer.valueOf((String) seguimientoInfluenzaJSON
								.get("controlDia"))) ;	
	
						if ((query.uniqueResult() != null))
							seguimientoInfluenza = (SeguimientoInfluenza) query
									.uniqueResult();
						
						seguimientoInfluenza.setSecHojaInfluenza(hojaInfluenza.getSecHojaInfluenza());
						
						seguimientoInfluenza.setControlDia(Integer.valueOf((String) seguimientoInfluenzaJSON
								.get("controlDia")));
						seguimientoInfluenza.setFechaSeguimiento(df.parse(
								seguimientoInfluenzaJSON.get("fechaSeguimiento").toString()));
						seguimientoInfluenza.setUsuarioMedico(((Number) seguimientoInfluenzaJSON.
								get("usuarioMedico")).shortValue());
	
						seguimientoInfluenza
								.setConsultaInicial(((String) seguimientoInfluenzaJSON
										.get("consultaInicial")));
						seguimientoInfluenza.setFiebre(((String) seguimientoInfluenzaJSON
								.get("fiebre")));
						seguimientoInfluenza.setTos(((String) seguimientoInfluenzaJSON
								.get("tos")));
						seguimientoInfluenza
								.setSecrecionNasal(((String) seguimientoInfluenzaJSON
										.get("secrecionNasal")));
						seguimientoInfluenza
								.setDolorGarganta(((String) seguimientoInfluenzaJSON
										.get("dolorGarganta")));
						seguimientoInfluenza
								.setCongestionNasa(((String) seguimientoInfluenzaJSON
										.get("congestionNasa")));
						seguimientoInfluenza
								.setDolorCabeza(((String) seguimientoInfluenzaJSON
										.get("dolorCabeza")));
						seguimientoInfluenza
								.setFaltaApetito(((String) seguimientoInfluenzaJSON
										.get("faltaApetito")));
						seguimientoInfluenza
								.setDolorMuscular(((String) seguimientoInfluenzaJSON
										.get("dolorMuscular")));
						seguimientoInfluenza
								.setDolorArticular(((String) seguimientoInfluenzaJSON
										.get("dolorArticular")));
						seguimientoInfluenza.setDolorOido(((String) seguimientoInfluenzaJSON
								.get("dolorOido")));
						seguimientoInfluenza
								.setRespiracionRapida(((String) seguimientoInfluenzaJSON
										.get("respiracionRapida")));
						seguimientoInfluenza
								.setDificultadRespirar(((String) seguimientoInfluenzaJSON
										.get("dificultadRespirar")));
						seguimientoInfluenza
								.setFaltaEscuela(((String) seguimientoInfluenzaJSON
										.get("faltaEscuela")));
						seguimientoInfluenza
								.setQuedoEnCama(((String) seguimientoInfluenzaJSON
										.get("quedoEnCama")));
	
						HIBERNATE_RESOURCE.begin();
						HIBERNATE_RESOURCE.getSession().saveOrUpdate(
								seguimientoInfluenza);
						HIBERNATE_RESOURCE.commit();
					}
				}
				List oLista = new LinkedList();
				Map fila = null;
				fila = new HashMap();
				fila.put("numHojaSeguimiento",
						hojaInfluenza.getNumHojaSeguimiento());
				oLista.add(fila);
				result = UtilResultado.parserResultado(oLista, "", UtilResultado.OK);
			} else {
				result = UtilResultado.parserResultado(null, Mensajes.HOJA_INFLUENZA_CERRADA, UtilResultado.INFO);
			}
		} catch (Exception e) {
			e.printStackTrace();
			result = UtilResultado.parserResultado(null,
					Mensajes.ERROR_NO_CONTROLADO + e.getMessage(),
					UtilResultado.ERROR);
			HIBERNATE_RESOURCE.rollback();
			// TODO: handle exception
		} finally {
			if (HIBERNATE_RESOURCE.getSession().isOpen()) {
				HIBERNATE_RESOURCE.close();
			}
		}
		return result;
	}

	/***
	 * Metodo para obtener el detalle del seguimiento influenza.
	 * @param paramSecHojaInfluenza, JSON con el Id del seguimiento influenza.
	 */
	@Override
	public String getListaSeguimientoInfluenza(int paramSecHojaInfluenza) {
		String result = null;
		try {

			List oLista = new LinkedList();
			Map fila = null;

			String sql = "select  "
					+ " to_char(s.fechaSeguimiento, 'dd/MM/yyyy'), "
					+ " s.controlDia, " + " u.nombre, "
					+ " s.consultaInicial, " + " s.fiebre, " + " s.tos, "
					+ " s.secrecionNasal, " + " s.dolorGarganta, "
					+ " s.congestionNasa, " + " s.dolorCabeza, "
					+ " s.faltaApetito, " + " s.dolorMuscular, "
					+ " s.dolorArticular, " + " s.dolorOido, "
					+ " s.respiracionRapida, " + " s.dificultadRespirar, "
					+ " s.faltaEscuela, " + " s.quedoEnCama, "
					+ " s.usuarioMedico "
					+ " from SeguimientoInfluenza s, UsuariosView u "
					+ " where s.usuarioMedico = u.id ";

			sql += " and s.secHojaInfluenza = :secHojaInfluenza ";

			sql += "order by s.controlDia asc";

			Query query = HIBERNATE_RESOURCE.getSession().createQuery(sql);

			query.setParameter("secHojaInfluenza", paramSecHojaInfluenza);

			List<Object[]> objLista = (List<Object[]>) query.list();

			if (objLista != null && objLista.size() > 0) {

				for (Object[] object : objLista) {

					// Construir la fila del registro actual usando arreglos

					fila = new HashMap();
					fila.put("fechaSeguimiento", object[0].toString());
					fila.put("controlDia", Integer.valueOf(object[1].toString()));
					fila.put("nombreMedico", object[2].toString());
					fila.put("consultaInicial", object[3].toString());
					fila.put("fiebre", object[4].toString());
					fila.put("tos", object[5].toString());
					fila.put("secrecionNasal", object[6].toString());
					fila.put("dolorGarganta", object[7].toString());
					fila.put("congestionNasa", object[8].toString());
					fila.put("dolorCabeza", object[9].toString());
					fila.put("faltaApetito", object[10].toString());
					fila.put("dolorMuscular", object[11].toString());
					fila.put("dolorArticular", object[12].toString());
					fila.put("dolorOido", object[13].toString());
					fila.put("respiracionRapida", object[14].toString());
					fila.put("dificultadRespirar", object[15].toString());
					fila.put("faltaEscuela", object[16].toString());
					fila.put("quedoEnCama", object[17].toString());
					fila.put("usuarioMedico", Integer.parseInt(object[18].toString()));
					oLista.add(fila);

				}

				// Construir la lista a una estructura JSON
				result = UtilResultado.parserResultado(oLista, "",
						UtilResultado.OK);
			} else {
				result = UtilResultado.parserResultado(null, Mensajes.NO_DATOS,
						UtilResultado.INFO);
			}

		} catch (Exception e) {
			e.printStackTrace();
			result = UtilResultado.parserResultado(null,
					Mensajes.ERROR_NO_CONTROLADO, UtilResultado.ERROR);
		} finally {
			if (HIBERNATE_RESOURCE.getSession().isOpen()) {
				HIBERNATE_RESOURCE.close();
			}
		}
		return result;
	}

	public byte[] getSeguimientoInfluenzaPdf(int numHojaSeguimiento) {

		String nombreReporte = "rptSeguimientoInfluenza";
		try {
			List oLista = new LinkedList(); // Listado final para el resultado

			/*
			 * String sql =
			 * "select p.cod_expediente \"codExpediente\", p.nombre1, p.nombre2, p.apellido1, p.apellido2, "
			 * +
			 * "h.num_hoja_seguimiento \"numHojaSeguimiento\", h.fis, h.fif, h.fecha_inicio \"fechaInicio\", h.fecha_cierre \"fechaCierre\", "
			 * +
			 * "s.control_dia \"controlDia\", s.fecha_seguimiento \"fechaSeguimiento\", u.nombre \"nombreMedico\", s.consulta_inicial \"consultaInicial\", "
			 * +
			 * "s.fiebre, s.tos, s.secrecion_nasal \"secrecionNasal\", s.dolor_garganta \"dolorGarganta\", congestion_nasa \"congestionNasa\", "
			 * +
			 * "s.dolor_cabeza \"dolorCabeza\", s.falta_apetito \"faltaApetito\", s.dolor_muscular \"dolorMuscular\", s.dolor_articular \"dolorArticular\", "
			 * +
			 * "s.dolor_oido \"dolorOido\", s.respiracion_rapida \"respiracionRapida\", s.dificultad_respirar \"dificultadRespirar\", s.falta_escuela \"faltaEscuela\", s.quedo_en_cama \"quedoEnCama\" "
			 * + "from paciente p " +
			 * "inner join hoja_influenza h on p.cod_expediente = h.cod_expediente "
			 * +
			 * "inner join seguimiento_influenza s on h.sec_hoja_influenza = s.sec_hoja_influenza "
			 * + "inner join usuarios_view u on s.usuario_medico = u.id ";
			 */

			String sql = " select distinct p.cod_expediente \"codExpediente\", p.nombre1, p.nombre2, p.apellido1, p.apellido2, "
					+ " h.num_hoja_seguimiento \"numHojaSeguimiento\", "
					+ " h.fis, h.fif, h.fecha_inicio \"fechaInicio\", "
					+ " h.fecha_cierre  \"fechaCierre\", "
					+ " s1.consulta_inicial \"consultaInicialDia1\", "
					+ " s2.consulta_inicial \"consultaInicialDia2\", "
					+ " s3.consulta_inicial \"consultaInicialDia3\", "
					+ " s4.consulta_inicial \"consultaInicialDia4\", "
					+ " s5.consulta_inicial \"consultaInicialDia5\", "
					+ " s6.consulta_inicial \"consultaInicialDia6\", "
					+ " s7.consulta_inicial \"consultaInicialDia7\", "
					+ " s8.consulta_inicial \"consultaInicialDia8\", "
					+ " s9.consulta_inicial \"consultaInicialDia9\", "
					+ " s10.consulta_inicial \"consultaInicialDia10\", "
					+ " s11.consulta_inicial \"consultaInicialDia11\", "
					+ " s12.consulta_inicial \"consultaInicialDia12\", "
					+ " s1.fiebre \"fiebreDia1\", "
					+ " s2.fiebre \"fiebreDia2\", "
					+ " s3.fiebre \"fiebreDia3\", "
					+ " s4.fiebre \"fiebreDia4\", "
					+ " s5.fiebre \"fiebreDia5\", "
					+ " s6.fiebre \"fiebreDia6\", "
					+ " s7.fiebre \"fiebreDia7\", "
					+ " s8.fiebre \"fiebreDia8\", "
					+ " s9.fiebre \"fiebreDia9\", "
					+ " s10.fiebre \"fiebreDia10\", "
					+ " s11.fiebre \"fiebreDia11\", "
					+ " s12.fiebre \"fiebreDia12\", "
					+ " s1.tos \"tosDia1\", "
					+ " s2.tos \"tosDia2\", "
					+ " s3.tos \"tosDia3\", "
					+ " s4.tos \"tosDia4\", "
					+ " s5.tos \"tosDia5\", "
					+ " s6.tos \"tosDia6\", "
					+ " s7.tos \"tosDia7\", "
					+ " s8.tos \"tosDia8\", "
					+ " s9.tos \"tosDia9\", "
					+ " s10.tos \"tosDia10\", "
					+ " s11.tos \"tosDia11\", "
					+ " s12.tos \"tosDia12\", "
					+ " s1.secrecion_nasal \"secrecionNasalDia1\", "
					+ " s2.secrecion_nasal \"secrecionNasalDia2\", "
					+ " s3.secrecion_nasal \"secrecionNasalDia3\", "
					+ " s4.secrecion_nasal \"secrecionNasalDia4\", "
					+ " s5.secrecion_nasal \"secrecionNasalDia5\", "
					+ " s6.secrecion_nasal \"secrecionNasalDia6\", "
					+ " s7.secrecion_nasal \"secrecionNasalDia7\", "
					+ " s8.secrecion_nasal \"secrecionNasalDia8\", "
					+ " s9.secrecion_nasal \"secrecionNasalDia9\", "
					+ " s10.secrecion_nasal \"secrecionNasalDia10\", "
					+ " s11.secrecion_nasal \"secrecionNasalDia11\", "
					+ " s12.secrecion_nasal \"secrecionNasalDia12\", "
					+ " s1.congestion_nasa \"congestionNasaDia1\", "
					+ " s2.congestion_nasa \"congestionNasaDia2\", "
					+ " s3.congestion_nasa \"congestionNasaDia3\", "
					+ " s4.congestion_nasa \"congestionNasaDia4\", "
					+ " s5.congestion_nasa \"congestionNasaDia5\", "
					+ " s6.congestion_nasa \"congestionNasaDia6\", "
					+ " s7.congestion_nasa \"congestionNasaDia7\", "
					+ " s8.congestion_nasa \"congestionNasaDia8\", "
					+ " s9.congestion_nasa \"congestionNasaDia9\", "
					+ " s10.congestion_nasa \"congestionNasaDia10\", "
					+ " s11.congestion_nasa \"congestionNasaDia11\", "
					+ " s12.congestion_nasa \"congestionNasaDia12\", "
					+ " s1.dolor_garganta \"dolorGargantaDia1\", "
					+ " s2.dolor_garganta \"dolorGargantaDia2\", "
					+ " s3.dolor_garganta \"dolorGargantaDia3\", "
					+ " s4.dolor_garganta \"dolorGargantaDia4\", "
					+ " s5.dolor_garganta \"dolorGargantaDia5\", "
					+ " s6.dolor_garganta \"dolorGargantaDia6\", "
					+ " s7.dolor_garganta \"dolorGargantaDia7\", "
					+ " s8.dolor_garganta \"dolorGargantaDia8\", "
					+ " s9.dolor_garganta \"dolorGargantaDia9\", "
					+ " s10.dolor_garganta \"dolorGargantaDia10\", "
					+ " s11.dolor_garganta \"dolorGargantaDia11\", "
					+ " s12.dolor_garganta \"dolorGargantaDia12\", "
					+ " s1.falta_apetito \"faltaApetitoDia1\", "
					+ " s2.falta_apetito \"faltaApetitoDia2\", "
					+ " s3.falta_apetito \"faltaApetitoDia3\", "
					+ " s4.falta_apetito \"faltaApetitoDia4\", "
					+ " s5.falta_apetito \"faltaApetitoDia5\", "
					+ " s6.falta_apetito \"faltaApetitoDia6\", "
					+ " s7.falta_apetito \"faltaApetitoDia7\", "
					+ " s8.falta_apetito \"faltaApetitoDia8\", "
					+ " s9.falta_apetito \"faltaApetitoDia9\", "
					+ " s10.falta_apetito \"faltaApetitoDia10\", "
					+ " s11.falta_apetito \"faltaApetitoDia11\", "
					+ " s12.falta_apetito \"faltaApetitoDia12\", "
					+ " s1.dolor_muscular \"dolorMuscularDia1\", "
					+ " s2.dolor_muscular \"dolorMuscularDia2\", "
					+ " s3.dolor_muscular \"dolorMuscularDia3\", "
					+ " s4.dolor_muscular \"dolorMuscularDia4\", "
					+ " s5.dolor_muscular \"dolorMuscularDia5\", "
					+ " s6.dolor_muscular \"dolorMuscularDia6\", "
					+ " s7.dolor_muscular \"dolorMuscularDia7\", "
					+ " s8.dolor_muscular \"dolorMuscularDia8\", "
					+ " s9.dolor_muscular \"dolorMuscularDia9\", "
					+ " s10.dolor_muscular \"dolorMuscularDia10\", "
					+ " s11.dolor_muscular \"dolorMuscularDia11\", "
					+ " s12.dolor_muscular \"dolorMuscularDia12\", "
					+ " s1.dolor_articular \"dolorArticularDia1\", "
					+ " s2.dolor_articular \"dolorArticularDia2\", "
					+ " s3.dolor_articular \"dolorArticularDia3\", "
					+ " s4.dolor_articular \"dolorArticularDia4\", "
					+ " s5.dolor_articular \"dolorArticularDia5\", "
					+ " s6.dolor_articular \"dolorArticularDia6\", "
					+ " s7.dolor_articular \"dolorArticularDia7\", "
					+ " s8.dolor_articular \"dolorArticularDia8\", "
					+ " s9.dolor_articular \"dolorArticularDia9\", "
					+ " s10.dolor_articular \"dolorArticularDia10\", "
					+ " s11.dolor_articular \"dolorArticularDia11\", "
					+ " s12.dolor_articular \"dolorArticularDia12\", "
					+ " s1.dolor_oido \"dolorOidoDia1\", "
					+ " s2.dolor_oido \"dolorOidoDia2\", "
					+ " s3.dolor_oido \"dolorOidoDia3\", "
					+ " s4.dolor_oido \"dolorOidoDia4\", "
					+ " s5.dolor_oido \"dolorOidoDia5\", "
					+ " s6.dolor_oido \"dolorOidoDia6\", "
					+ " s7.dolor_oido \"dolorOidoDia7\", "
					+ " s8.dolor_oido \"dolorOidoDia8\", "
					+ " s9.dolor_oido \"dolorOidoDia9\", "
					+ " s10.dolor_oido \"dolorOidoDia10\", "
					+ " s11.dolor_oido \"dolorOidoDia11\", "
					+ " s12.dolor_oido \"dolorOidoDia12\", "
					+ " s1.respiracion_rapida \"respiracionRapidaDia1\", "
					+ " s2.respiracion_rapida \"respiracionRapidaDia2\", "
					+ " s3.respiracion_rapida \"respiracionRapidaDia3\", "
					+ " s4.respiracion_rapida \"respiracionRapidaDia4\", "
					+ " s5.respiracion_rapida \"respiracionRapidaDia5\", "
					+ " s6.respiracion_rapida \"respiracionRapidaDia6\", "
					+ " s7.respiracion_rapida \"respiracionRapidaDia7\", "
					+ " s8.respiracion_rapida \"respiracionRapidaDia8\", "
					+ " s9.respiracion_rapida \"respiracionRapidaDia9\", "
					+ " s10.respiracion_rapida \"respiracionRapidaDia10\", "
					+ " s11.respiracion_rapida \"respiracionRapidaDia11\", "
					+ " s12.respiracion_rapida \"respiracionRapidaDia12\", "
					+ " s1.dificultad_respirar \"dificultadRespirarDia1\", "
					+ " s2.dificultad_respirar \"dificultadRespirarDia2\", "
					+ " s3.dificultad_respirar \"dificultadRespirarDia3\", "
					+ " s4.dificultad_respirar \"dificultadRespirarDia4\", "
					+ " s5.dificultad_respirar \"dificultadRespirarDia5\", "
					+ " s6.dificultad_respirar \"dificultadRespirarDia6\", "
					+ " s7.dificultad_respirar \"dificultadRespirarDia7\", "
					+ " s8.dificultad_respirar \"dificultadRespirarDia8\", "
					+ " s9.dificultad_respirar \"dificultadRespirarDia9\", "
					+ " s10.dificultad_respirar \"dificultadRespirarDia10\", "
					+ " s11.dificultad_respirar \"dificultadRespirarDia11\", "
					+ " s12.dificultad_respirar \"dificultadRespirarDia12\", "
					+

					" s1.falta_escuela \"faltaEscuelaDia1\", "
					+ " s2.falta_escuela \"faltaEscuelaDia2\", "
					+ " s3.falta_escuela \"faltaEscuelaDia3\", "
					+ " s4.falta_escuela \"faltaEscuelaDia4\", "
					+ " s5.falta_escuela \"faltaEscuelaDia5\", "
					+ " s6.falta_escuela \"faltaEscuelaDia6\", "
					+ " s7.falta_escuela \"faltaEscuelaDia7\", "
					+ " s8.falta_escuela \"faltaEscuelaDia8\", "
					+ " s9.falta_escuela \"faltaEscuelaDia9\", "
					+ " s10.falta_escuela \"faltaEscuelaDia10\", "
					+ " s11.falta_escuela \"faltaEscuelaDia11\", "
					+ " s12.falta_escuela \"faltaEscuelaDia12\", "
					+

					" s1.quedo_en_cama \"quedoEnCamaDia1\", "
					+ " s2.quedo_en_cama \"quedoEnCamaDia2\", "
					+ " s3.quedo_en_cama \"quedoEnCamaDia3\", "
					+ " s4.quedo_en_cama \"quedoEnCamaDia4\", "
					+ " s5.quedo_en_cama \"quedoEnCamaDia5\", "
					+ " s6.quedo_en_cama \"quedoEnCamaDia6\", "
					+ " s7.quedo_en_cama \"quedoEnCamaDia7\", "
					+ " s8.quedo_en_cama \"quedoEnCamaDia8\", "
					+ " s9.quedo_en_cama \"quedoEnCamaDia9\", "
					+ " s10.quedo_en_cama \"quedoEnCamaDia10\", "
					+ " s11.quedo_en_cama \"quedoEnCamaDia11\", "
					+ " s12.quedo_en_cama \"quedoEnCamaDia12\", "
					+ " (select um.codigopersonal from usuarios_view um where s1.usuario_medico = um.id) \"nombreMedico1\", "
					+ " (select um.codigopersonal from usuarios_view um where s2.usuario_medico = um.id) \"nombreMedico2\", "
					+ " (select um.codigopersonal from usuarios_view um where s3.usuario_medico = um.id) \"nombreMedico3\", "
					+ " (select um.codigopersonal from usuarios_view um where s4.usuario_medico = um.id) \"nombreMedico4\", "
					+ " (select um.codigopersonal from usuarios_view um where s5.usuario_medico = um.id) \"nombreMedico5\", "
					+ " (select um.codigopersonal from usuarios_view um where s6.usuario_medico = um.id) \"nombreMedico6\", "
					+ " (select um.codigopersonal from usuarios_view um where s7.usuario_medico = um.id) \"nombreMedico7\", "
					+ " (select um.codigopersonal from usuarios_view um where s8.usuario_medico = um.id) \"nombreMedico8\", "
					+ " (select um.codigopersonal from usuarios_view um where s9.usuario_medico = um.id) \"nombreMedico9\", "
					+ " (select um.codigopersonal from usuarios_view um where s10.usuario_medico = um.id) \"nombreMedico10\", "
					+ " (select um.codigopersonal from usuarios_view um where s11.usuario_medico = um.id) \"nombreMedico11\", "
					+ " (select um.codigopersonal from usuarios_view um where s12.usuario_medico = um.id) \"nombreMedico12\", "
					+ " to_char(s1.fecha_seguimiento, 'dd/MM/yyyy') \"fechaSeguimiento1\", "
					+ " to_char(s2.fecha_seguimiento, 'dd/MM/yyyy') \"fechaSeguimiento2\", "
					+ " to_char(s3.fecha_seguimiento, 'dd/MM/yyyy') \"fechaSeguimiento3\", "
					+ " to_char(s4.fecha_seguimiento, 'dd/MM/yyyy') \"fechaSeguimiento4\", "
					+ " to_char(s5.fecha_seguimiento, 'dd/MM/yyyy') \"fechaSeguimiento5\", "
					+ " to_char(s6.fecha_seguimiento, 'dd/MM/yyyy') \"fechaSeguimiento6\", "
					+ " to_char(s7.fecha_seguimiento, 'dd/MM/yyyy') \"fechaSeguimiento7\", "
					+ " to_char(s8.fecha_seguimiento, 'dd/MM/yyyy') \"fechaSeguimiento8\", "
					+ " to_char(s9.fecha_seguimiento, 'dd/MM/yyyy') \"fechaSeguimiento9\", "
					+ " to_char(s10.fecha_seguimiento, 'dd/MM/yyyy') \"fechaSeguimiento10\", "
					+ " to_char(s11.fecha_seguimiento, 'dd/MM/yyyy') \"fechaSeguimiento11\", "
					+ " to_char(s12.fecha_seguimiento, 'dd/MM/yyyy') \"fechaSeguimiento12\", "
					+" s1.dolor_cabeza \"dolorCabezaDia1\", "
					+ " s2.dolor_cabeza \"dolorCabezaDia2\", "
					+ " s3.dolor_cabeza \"dolorCabezaDia3\", "
					+ " s4.dolor_cabeza \"dolorCabezaDia4\", "
					+ " s5.dolor_cabeza \"dolorCabezaDia5\", "
					+ " s6.dolor_cabeza \"dolorCabezaDia6\", "
					+ " s7.dolor_cabeza \"dolorCabezaDia7\", "
					+ " s8.dolor_cabeza \"dolorCabezaDia8\", "
					+ " s9.dolor_cabeza \"dolorCabezaDia9\", "
					+ " s10.dolor_cabeza \"dolorCabezaDia10\", "
					+ " s11.dolor_cabeza \"dolorCabezaDia11\", "
					+ " s12.dolor_cabeza \"dolorCabezaDia12\" "
					+ " from paciente p  "
					+ " inner join hoja_influenza h on p.cod_expediente = h.cod_expediente "
					+ " inner join seguimiento_influenza s on h.sec_hoja_influenza = s.sec_hoja_influenza "
					+ " left join seguimiento_influenza s1 on h.sec_hoja_influenza = s1.sec_hoja_influenza and s1.control_dia='1' "
					+ " left join seguimiento_influenza s2 on h.sec_hoja_influenza = s2.sec_hoja_influenza and s2.control_dia='2' "
					+ " left join seguimiento_influenza s3 on h.sec_hoja_influenza = s3.sec_hoja_influenza and s3.control_dia='3' "
					+ " left join seguimiento_influenza s4 on h.sec_hoja_influenza = s4.sec_hoja_influenza and s4.control_dia='4' "
					+ " left join seguimiento_influenza s5 on h.sec_hoja_influenza = s5.sec_hoja_influenza and s5.control_dia='5' "
					+ " left join seguimiento_influenza s6 on h.sec_hoja_influenza = s6.sec_hoja_influenza and s6.control_dia='6' "
					+ " left join seguimiento_influenza s7 on h.sec_hoja_influenza = s7.sec_hoja_influenza and s7.control_dia='7' "
					+ " left join seguimiento_influenza s8 on h.sec_hoja_influenza = s8.sec_hoja_influenza and s8.control_dia='8' "
					+ " left join seguimiento_influenza s9 on h.sec_hoja_influenza = s9.sec_hoja_influenza and s9.control_dia='9' "
					+ " left join seguimiento_influenza s10 on h.sec_hoja_influenza = s10.sec_hoja_influenza and s10.control_dia='10' "
					+ " left join seguimiento_influenza s11 on h.sec_hoja_influenza = s11.sec_hoja_influenza and s11.control_dia='11' "
					+ " left join seguimiento_influenza s12 on h.sec_hoja_influenza = s12.sec_hoja_influenza and s12.control_dia='12' ";

			sql += " where  h.num_hoja_seguimiento = :numHojaSeguimiento ";
			// System.out.println(sql);
			Query query = HIBERNATE_RESOURCE
					.getSession()
					.createSQLQuery(sql)
					.setResultTransformer(
							Transformers
									.aliasToBean(SeguimientoInfluenzaReporte.class))
					.setParameter("numHojaSeguimiento", numHojaSeguimiento);

			List result = query.list();

			return UtilitarioReporte.mostrarReporte(nombreReporte, null,
					result, false, null);

		} catch (Exception e) {
			e.printStackTrace();

		} finally {
			if (HIBERNATE_RESOURCE.getSession().isOpen()) {
				HIBERNATE_RESOURCE.close();
			}
		}
		return null;
	}

	/***
	 * Metodo que realiza la impresion de Seguimiento Influenza.
	 * @param numHojaSeguimiento, Numero de seguimiento influenza.
	 */
	public void imprimirSeguimientoInfluenciaPdf(int numHojaSeguimiento) {

		UtilitarioReporte ureporte = new UtilitarioReporte();
		ureporte.imprimirDocumento("rptSeguimientoInfluenza_"
				+ numHojaSeguimiento,
				getSeguimientoInfluenzaPdf(numHojaSeguimiento));

	}
	
	@Override
	public String reimpresionHojaConsulta(int paramsecHojaConsulta) {
		String result = null;
		try {
			consultaReporteService = new HojaConsultaReporteDA();

			try {
            	consultaReporteService.imprimirConsultaPdf(paramsecHojaConsulta);
            } catch(Exception e) {
            	e.printStackTrace();
            }
			result = UtilResultado.parserResultado(null, "", UtilResultado.OK);
			
		} catch (Exception e) {
			e.printStackTrace();
			result = UtilResultado.parserResultado(null,
					Mensajes.ERROR_NO_CONTROLADO + e.getMessage(),
					UtilResultado.ERROR);
			HIBERNATE_RESOURCE.rollback();
			// TODO: handle exception
		} finally {
			if (HIBERNATE_RESOURCE.getSession().isOpen()) {
				HIBERNATE_RESOURCE.close();
			}
		}
		return result;
	}
	
	/***
	 * Metodo para obtener el detalle del seguimiento zika.
	 * @param paramSecHojaZika, JSON con el Id del seguimiento zika.
	 */
	@Override
	public String getListaSeguimientoZika(int paramSecHojaZika) {
		String result = null;
		try {

			List oLista = new LinkedList();
			Map fila = null;

			String sql = "select  "
					+ " to_char(z.fechaSeguimiento, 'dd/MM/yyyy'), "
					+ " z.controlDia, " + " u.nombre, " 
					+ " z.consultaInicial, " + " z.fiebre, " + " z.astenia, "
					+ " z.malEstadoGral, " + "z.escalosfrios,"
					+ " z.convulsiones, " + " z.cefalea, " + "z.rigidezCuello,"
					+ " z.dolorRetroocular, " + " z.pocoApetito, " + "z.nauseas,"
					+ " z.vomitos, " + " z.diarrea, " + "z.dolorAbdominalContinuo,"
					+ " z.artralgiaProximal, " + " z.artralgiaDistal, " + "z.mialgia,"
					+ " z.conjuntivitisNoPurulenta, " + " z.edemaArtProxMS, " + "z.edemaArtDistMS,"
					+ " z.edemaArtProxMI, " + " z.edemaArtDistMI, " + "z.edemaPeriauricular,"
					+ " z.adenopatiaCervAnt, " + " z.adenopatiaCervPost, " + "z.adenopatiaRetroAuricular,"
					+ " z.rash, " + " z.equimosis, " + "z.pruebaTorniquetePos,"
					+ " z.epistaxis, " + " z.gingivorragia, " + "z.petequiasEspontaneas,"
					+ " z.hematemesis, " + " z.melena, " + "z.oftalmoplejia,"
					+ " z.dificultadResp, " + " z.debilidadMuscMS, " + "z.debilidadMuscMI,"
					+ " z.parestesiaMS, " + " z.parestesiaMI, " + "z.paralisisMuscMS,"
					+ " z.paralisisMuscMI, " + " z.tos, " + "z.rinorrea,"
					+ " z.dolorGarganta, " + " z.prurito, " 
					+ " z.usuarioMedico, " + "z.supervisor"
					+ " from SeguimientoZika z, UsuariosView u "
					+ " where z.usuarioMedico = u.id ";

			sql += " and z.secHojaZika = :secHojaZika ";

			sql += "order by z.controlDia asc";

			Query query = HIBERNATE_RESOURCE.getSession().createQuery(sql);

			query.setParameter("secHojaZika", paramSecHojaZika);

			List<Object[]> objLista = (List<Object[]>) query.list();

			if (objLista != null && objLista.size() > 0) {

				for (Object[] object : objLista) {

					// Construir la fila del registro actual usando arreglos

					fila = new HashMap();
					fila.put("fechaSeguimiento", object[0].toString());
					fila.put("controlDia", Integer.valueOf(object[1].toString()));
					fila.put("nombreMedico", object[2].toString());
					fila.put("consultaInicial", object[3].toString());
					fila.put("fiebre", object[4].toString());
					fila.put("astenia", object[5].toString());
					fila.put("malEstadoGral", object[6].toString());
					fila.put("escalosfrios", object[7].toString());
					fila.put("convulsiones", object[8].toString());
					fila.put("cefalea", object[9].toString());
					fila.put("rigidezCuello", object[10].toString());
					fila.put("dolorRetroocular", object[11].toString());
					fila.put("pocoApetito", object[12].toString());
					fila.put("nauseas", object[13].toString());
					fila.put("vomitos", object[14].toString());
					fila.put("diarrea", object[15].toString());
					fila.put("dolorAbdominalContinuo", object[16].toString());
					fila.put("artralgiaProximal", object[17].toString());
					fila.put("artralgiaDistal", object[18].toString());
					fila.put("mialgia", object[19].toString());
					fila.put("conjuntivitisNoPurulenta", object[20].toString());
					fila.put("edemaArtProxMS", object[21].toString());
					fila.put("edemaArtDistMS", object[22].toString());
					fila.put("edemaArtProxMI", object[23].toString());
					fila.put("edemaArtDistMI", object[24].toString());
					fila.put("edemaPeriauricular", object[25].toString());
					fila.put("adenopatiaCervAnt", object[26].toString());
					fila.put("adenopatiaCervPost", object[27].toString());
					fila.put("adenopatiaRetroAuricular", object[28].toString());
					fila.put("rash", object[29].toString());
					fila.put("equimosis", object[30].toString());
					fila.put("pruebaTorniquetePos", object[31].toString());
					fila.put("epistaxis", object[32].toString());
					fila.put("gingivorragia", object[33].toString());
					fila.put("petequiasEspontaneas", object[34].toString());
					fila.put("hematemesis", object[35].toString());
					fila.put("melena", object[36].toString());
					fila.put("oftalmoplejia", object[37].toString());
					fila.put("dificultadResp", object[38].toString());
					fila.put("debilidadMuscMS", object[39].toString());
					fila.put("debilidadMuscMI", object[40].toString());
					fila.put("parestesiaMS", object[41].toString());
					fila.put("parestesiaMI", object[42].toString());
					fila.put("paralisisMuscMS", object[43].toString());
					fila.put("paralisisMuscMI", object[44].toString());
					fila.put("tos", object[45].toString());
					fila.put("rinorrea", object[46].toString());
					fila.put("dolorGarganta", object[47].toString());
					fila.put("prurito", object[48].toString());
					fila.put("usuarioMedico", Integer.parseInt(object[49].toString()));
					fila.put("supervisor", Integer.parseInt(object[50].toString()));
					oLista.add(fila);

				}

				// Construir la lista a una estructura JSON
				result = UtilResultado.parserResultado(oLista, "",
						UtilResultado.OK);
			} else {
				result = UtilResultado.parserResultado(null, Mensajes.NO_DATOS,
						UtilResultado.INFO);
			}

		} catch (Exception e) {
			e.printStackTrace();
			result = UtilResultado.parserResultado(null,
					Mensajes.ERROR_NO_CONTROLADO, UtilResultado.ERROR);
		} finally {
			if (HIBERNATE_RESOURCE.getSession().isOpen()) {
				HIBERNATE_RESOURCE.close();
			}
		}
		return result;
	}
	
	/***
	 * Funcion para buscar paciente con hoja de seguimiento zika por codigo de expediente.
	 * @param codExpediente, Codigo Expediente.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public String buscarPacienteSeguimientoZika(int codExpediente) {
		String result = null;
		try {
			List oLista = new LinkedList();
			Map fila = null;

			String sql = "select p.nombre1, p.nombre2, " + 
					" p.apellido1, p.apellido2, p.codExpediente, hs.numHojaSeguimiento, " + 
					
					" hs.fif, hs.fis, to_char(hs.fechaInicio, 'yyyyMMdd'), to_char(hs.fechaCierre, 'yyyyMMdd'), " + 
					" hs.secHojaZika, hs.cerrado, hs.categoria, " + 
					" hs.sintomaInicial1, hs.sintomaInicial2, hs.sintomaInicial3, hs.sintomaInicial4 " +
					" from Paciente p, HojaZika hs, ConsEstudios ce " + 
					" where p.codExpediente = :codExpediente " + 
					" and p.codExpediente = hs.codExpediente " +
					" and p.codExpediente = ce.codigoExpediente " +
					" and ce.retirado != '1' order by hs.secHojaZika desc";

			Query query = HIBERNATE_RESOURCE.getSession().createQuery(sql);
			query.setParameter("codExpediente", codExpediente);
			query.setMaxResults(1);

			Object[] paciente = (Object[]) query.uniqueResult();

			if (paciente != null && paciente.length > 0) {
				
				sql = "select ec " + 
						" from ConsEstudios c, EstudioCatalogo ec " + 
						" where c.codigoConsentimiento = ec.codEstudio" + 
						" and c.codigoExpediente = :codExpediente " + 
						" and c.retirado != '1'" +
						" group by ec.codEstudio, ec.descEstudio";

				query = HIBERNATE_RESOURCE.getSession().createQuery(sql);

				query.setParameter("codExpediente", codExpediente);

				List<EstudioCatalogo> lstConsEstudios = (List<EstudioCatalogo>) query.list();
				StringBuffer codigosEstudios = new StringBuffer();

				for (EstudioCatalogo estudioCatalogo : lstConsEstudios) {
					codigosEstudios.append(estudioCatalogo.getDescEstudio()).append(",");
				}

				fila = new HashMap();
				// fila.put("nombrePaciente", paciente[0].toString()+ " " +
				// paciente[1].toString()+ " " + paciente[2].toString()+ " "
				// + paciente[3].toString());

				fila.put("nomPaciente", paciente[0].toString() + 
						" " + 
						((paciente[1] != null) ? paciente[1].toString() : "") + 
						" " + paciente[2].toString() + " " + 
						((paciente[3] != null) ? paciente[3].toString() : ""));
				
				fila.put("estudioPaciente", codigosEstudios != null && 
						!codigosEstudios.toString().isEmpty() ? 
								(codigosEstudios.substring(0, (codigosEstudios.length() - 1))): "");

				fila.put("codExpediente", ((Integer)paciente[4]).intValue());

				if (paciente.length > 5) {
					fila.put("numHojaSeguimiento", ((Integer)paciente[5]).intValue());
					fila.put("fif", (paciente[6] != null) ? paciente[6].toString() : "");
					fila.put("fis", (paciente[7] != null) ? paciente[7].toString() : "");
					fila.put("secHojaZika", ((Integer)paciente[10]).intValue());
					fila.put("cerrado", paciente[11].toString().charAt(0));
					fila.put("categoria", (paciente[12] != null) ? paciente[12].toString() : "");
					fila.put("sintomaInicial1", (paciente[13] != null) ? paciente[13].toString() : "");
					fila.put("sintomaInicial2", (paciente[14] != null) ? paciente[14].toString() : "");
					fila.put("sintomaInicial3", (paciente[15] != null) ? paciente[15].toString() : "");
					fila.put("sintomaInicial4", (paciente[16] != null) ? paciente[16].toString() : "");
				}

				oLista.add(fila);

				// Construir la lista a una estructura JSON
				result = UtilResultado.parserResultado(oLista, "",
						UtilResultado.OK);
			} else {
				result = UtilResultado.parserResultado(null,
						Mensajes.REGISTRO_NO_ENCONTRADO, UtilResultado.INFO);
			}

		} catch (Exception e) {
			e.printStackTrace();
			result = UtilResultado.parserResultado(null,
					Mensajes.ERROR_NO_CONTROLADO, UtilResultado.ERROR);
		} finally {
			if (HIBERNATE_RESOURCE.getSession().isOpen()) {
				HIBERNATE_RESOURCE.close();
			}
		}
		return result;
	}
	
	/***
	 * Funcion para buscar el seguimiento zika por numero de hoja de seguimiento.
	 * @param numHojaSeguimiento, Numero de seguimiento.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public String buscarHojaSeguimientoZika(int numHojaSeguimiento) {
		String result = null;
		try {
			List oLista = new LinkedList();
			Map fila = null;

			String sql = "select p.nombre1, p.nombre2, " + 
					" p.apellido1, p.apellido2, p.codExpediente, hs.numHojaSeguimiento, " +
					" hs.fif, hs.fis, " + 
					" hs.secHojaZika, hs.cerrado, hs.categoria, " + 
					" hs.sintomaInicial1, hs.sintomaInicial2, hs.sintomaInicial3, hs.sintomaInicial4 " +
					" from Paciente p, HojaZika hs, ConsEstudios ce " + 
					" where hs.numHojaSeguimiento = :numHojaSeguimiento " + 
					" and p.codExpediente = hs.codExpediente " +
					" and p.codExpediente = ce.codigoExpediente " +
					" and ce.retirado != '1' order by hs.secHojaZika desc";

			Query query = HIBERNATE_RESOURCE.getSession().createQuery(sql);
			query.setParameter("numHojaSeguimiento", numHojaSeguimiento);
			query.setMaxResults(1);

			Object[] paciente = (Object[]) query.uniqueResult();

			if (paciente != null && paciente.length > 0) {
				
				sql = "select ec " + 
						" from ConsEstudios c, EstudioCatalogo ec " + 
						" where c.codigoConsentimiento = ec.codEstudio" + 
						" and c.codigoExpediente = :codExpediente " + 
						" and c.retirado != '1'" +
						" group by ec.codEstudio, ec.descEstudio";

				query = HIBERNATE_RESOURCE.getSession().createQuery(sql);

				query.setParameter("codExpediente", ((Integer) paciente[4]).intValue());

				List<EstudioCatalogo> lstConsEstudios = (List<EstudioCatalogo>) query.list();
				StringBuffer codigosEstudios = new StringBuffer();

				for (EstudioCatalogo estudioCatalogo : lstConsEstudios) {
					codigosEstudios.append(estudioCatalogo.getDescEstudio()).append(",");
				}

				fila = new HashMap();
				fila.put("nomPaciente", paciente[0].toString() + 
						" " + ((paciente[1] != null) ? paciente[1].toString() : "") + 
						" " + paciente[2].toString() + " " + 
						((paciente[3] != null) ? paciente[3].toString() : ""));
				fila.put("estudioPaciente", (codigosEstudios != null && 
						!codigosEstudios.toString().isEmpty()) ? 
								(codigosEstudios.substring(0, (codigosEstudios.length() - 1))): "");
				fila.put("codExpediente", ((Integer)paciente[4]).intValue());
				fila.put("numHojaSeguimiento", ((Integer)paciente[5]).intValue());
				fila.put("fif", (paciente[6] != null) ? paciente[6].toString() : "");
				fila.put("fis", (paciente[7] != null) ? paciente[7].toString() : "");
				fila.put("secHojaInfluenza", ((Integer)paciente[8]).intValue());
				fila.put("cerrado", paciente[9].toString().charAt(0));
				fila.put("categoria", (paciente[10] != null) ? paciente[10].toString() : "");
				fila.put("sintomaIncial1", (paciente[11] != null) ? paciente[11].toString() : "");
				fila.put("sintomaInicial2", (paciente[12] != null) ? paciente[12].toString() : "");
				fila.put("sintomaInicial3", (paciente[13] != null) ? paciente[13].toString() : "");
				fila.put("sintomaInicial4", (paciente[14] != null) ? paciente[14].toString() : "");

				oLista.add(fila);

				// Construir la lista a una estructura JSON
				result = UtilResultado.parserResultado(oLista, "", UtilResultado.OK);
			} else {
				result = UtilResultado.parserResultado(null,
						Mensajes.REGISTRO_NO_ENCONTRADO, UtilResultado.INFO);
			}

		} catch (Exception e) {
			e.printStackTrace();
			result = UtilResultado.parserResultado(null,
					Mensajes.ERROR_NO_CONTROLADO, UtilResultado.ERROR);
		} finally {
			if (HIBERNATE_RESOURCE.getSession().isOpen()) {
				HIBERNATE_RESOURCE.close();
			}
		}
		return result;
	}
	
	/***
	 * Metodo para crear un nuevo seguimiento zika.
	 * @param paramCrearHoja, JSON con los parametros requeridos para crear seguimiento.
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public String crearSeguimientoZika(String paramCrearHoja) {
		String result = null;
		try {

			int codExpediente;
			String sql;
			Query query;
			HojaZika hojaZika;
			SeguimientoZika seguimientoZika;

			JSONParser parser = new JSONParser();
			Object obj = (Object) parser.parse(paramCrearHoja);
			JSONObject crearHojaJson = (JSONObject) obj;

			codExpediente = (((Number) crearHojaJson.get("codExpediente"))
					.intValue());
			
			//obtenemos la ultima hoja de consulta para el c�digo de expediente
			sql = "select h from HojaConsulta h " +
				 " where h.codExpediente = :codExpediente order by h.secHojaConsulta desc ";

			query = HIBERNATE_RESOURCE.getSession().createQuery(sql);
			query.setParameter("codExpediente", codExpediente);
			query.setMaxResults(1);
			
			HojaConsulta hojaConsulta = (HojaConsulta) query.uniqueResult();
			
			if(hojaConsulta == null){
				result = UtilResultado.parserResultado(null, Mensajes.NO_EXISTE_HC_CODEXP, UtilResultado.INFO);
			
			}else{
				
				if(hojaConsulta.getFif() == null || hojaConsulta.getFis() == null){
					return UtilResultado.parserResultado(null, Mensajes.HOJA_SIN_FIS_FIF, UtilResultado.INFO);
				}
				
				//verificando si tiene hojas abiertas
				sql = "select count(*) from hoja_zika where cerrado = 'N' and cod_expediente = :codExpediente";
				query = HIBERNATE_RESOURCE.getSession().createSQLQuery(sql);
				query.setParameter("codExpediente", codExpediente);
				
				BigInteger totalActivos = (BigInteger) query.uniqueResult();
				
				//Si tiene uno o mas activos retornamos aviso
				if(totalActivos.intValue() > 0){
					return UtilResultado.parserResultado(null, Mensajes.HOJA_ZIKA_NO_CERRADA, UtilResultado.INFO);
				}				
				
				String FIF, FIS;
				SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
				FIF = hojaConsulta.getFif() != null ? sdf.format(hojaConsulta.getFif()) : "";
				FIS = hojaConsulta.getFis() != null ? sdf.format(hojaConsulta.getFis()) : "";
			
				sql = "select max(h.numHojaSeguimiento) "
						+ " from HojaZika h ";
	
				query = HIBERNATE_RESOURCE.getSession().createQuery(sql);
	
				Integer maxNumHojaSeguimiento = (query.uniqueResult() == null) ? 1 : 
					((Integer) query.uniqueResult()).intValue() + 1;
				
				Calendar fechaInicio = Calendar.getInstance();
				Object objFechaInicio = (Object) parser.parse(crearHojaJson.get("fechaInicio").toString());
				JSONObject fechaInicioJson = (JSONObject) objFechaInicio;
				
				fechaInicio.set(((Number)fechaInicioJson.get("year")).intValue(), 
						((Number)fechaInicioJson.get("month")).intValue(), 
						((Number)fechaInicioJson.get("dayOfMonth")).intValue());
	
				hojaZika = new HojaZika();
				hojaZika.setNumHojaSeguimiento(maxNumHojaSeguimiento);
				hojaZika.setCodExpediente(codExpediente);
				hojaZika.setFechaInicio(fechaInicio.getTime());
				hojaZika.setCerrado('N');				
				hojaZika.setFif(FIF);
				hojaZika.setFis(FIS);
				
				HIBERNATE_RESOURCE.begin();
				HIBERNATE_RESOURCE.getSession().saveOrUpdate(hojaZika);
				HIBERNATE_RESOURCE.commit();
				
				List oLista = new LinkedList();
				Map fila = null;
				fila = new HashMap();
				fila.put("numHojaSeguimiento", hojaZika.getNumHojaSeguimiento());
				fila.put("codExpediente", hojaZika.getCodExpediente());
				fila.put("fif", FIF);
				fila.put("fis", FIS);
				oLista.add(fila);
				result = UtilResultado.parserResultado(oLista, "", UtilResultado.OK);
			}
		} catch (Exception e) {
			e.printStackTrace();
			result = UtilResultado.parserResultado(null, Mensajes.ERROR_NO_CONTROLADO + e.getMessage(),
					UtilResultado.ERROR);
			HIBERNATE_RESOURCE.rollback();
			// TODO: handle exception
		} finally {
			if (HIBERNATE_RESOURCE.getSession().isOpen()) {
				HIBERNATE_RESOURCE.close();
			}
		}
		return result;
	}
	
	/***
	 * Metodo para guardar la cabezera y detalle de seguimiento zika.
	 * @param, JSON Datos de la cabecera y detalle.
	 */
	@Override
	public String guardarSeguimientoZika(String paramHojaZika,
			String paramSeguimientoZika) {
		String result = null;
		try {

			int codExpediente;
			int numHojaSeguimiento;
			String sql;
			Query query;
			HojaZika hojaZika = new HojaZika();
			SeguimientoZika seguimientoZika;
			SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");

			JSONParser parser = new JSONParser();
			Object obj = (Object) parser.parse(paramHojaZika);
			JSONObject hojaZikaJSON = (JSONObject) obj;

			obj = new Object();
			obj = parser.parse(paramSeguimientoZika);
			JSONArray seguimientoZikaArray = (JSONArray) obj;

			codExpediente = (((Number) hojaZikaJSON.get("codExpediente"))
					.intValue());
			numHojaSeguimiento = ((Number) hojaZikaJSON
					.get("numHojaSeguimiento")).intValue();
					      
			if (numHojaSeguimiento == 0) {
//				sql = "select max(h.numHojaSeguimiento) "
//						+ " from HojaInfluenza h ";
//
//				query = HIBERNATE_RESOURCE.getSession().createQuery(sql);
//
//				Integer maxNumHojaSeguimiento = (query.uniqueResult() == null) ? 1
//						: ((Integer) query.uniqueResult()).intValue() + 1;
//
//				hojaInfluenza = new HojaInfluenza();
//				hojaInfluenza.setNumHojaSeguimiento(maxNumHojaSeguimiento);
//				hojaInfluenza.setCodExpediente(codExpediente);
//				hojaInfluenza.setFis(hojaInfluenzaJSON.get("fis").toString());
//				hojaInfluenza.setFif(hojaInfluenzaJSON.get("fif").toString());
//				if (hojaInfluenzaJSON.get("fechaCierre") != null)
//					hojaInfluenza.setFechaCierre(df.parse(hojaInfluenzaJSON
//							.get("fechaCierre").toString()));
//				hojaInfluenza.setCerrado(hojaInfluenzaJSON.get("cerrado")
//						.toString().charAt(0));
			} else {
				sql = "select h from HojaZika h " +
						" where h.codExpediente = :codExpediente " +
						" and h.numHojaSeguimiento = :numHojaSeguimiento";
				
				query = HIBERNATE_RESOURCE.getSession().createQuery(sql);
				query.setParameter("codExpediente", codExpediente);
				query.setParameter("numHojaSeguimiento", numHojaSeguimiento);

				hojaZika = ((HojaZika) query.uniqueResult());

				hojaZika.setNumHojaSeguimiento(numHojaSeguimiento);
				hojaZika.setCodExpediente(codExpediente);
				hojaZika.setFis(hojaZikaJSON.get("fis").toString());
				hojaZika.setFif(hojaZikaJSON.get("fif").toString());
				hojaZika.setCategoria(hojaZikaJSON.get("categoria").toString());
				hojaZika.setSintomaInicial1(hojaZikaJSON.get("sintomaInicial1").toString());
				
				if (hojaZikaJSON.get("sintomaInicial2") != null){
					hojaZika.setSintomaInicial2(hojaZikaJSON.get("sintomaInicial2").toString());
				}
				
				if (hojaZikaJSON.get("sintomaInicial3") != null){
					hojaZika.setSintomaInicial3(hojaZikaJSON.get("sintomaInicial3").toString());
				}
				
				if (hojaZikaJSON.get("sintomaInicial4") != null){
					hojaZika.setSintomaInicial4(hojaZikaJSON.get("sintomaInicial4").toString());
				}
							
				
				if (hojaZikaJSON.containsKey("fechaCierre") && 
						hojaZikaJSON.get("fechaCierre") != null) {
					hojaZika.setFechaCierre(df.parse(hojaZikaJSON
							.get("fechaCierre").toString()));
				}

			}
			
			if(hojaZika.getCerrado() != 'S' && hojaZika.getCerrado() != 's') {
				hojaZika.setCerrado(hojaZikaJSON.get("cerrado").toString().charAt(0));
				HIBERNATE_RESOURCE.begin();
				HIBERNATE_RESOURCE.getSession().saveOrUpdate(hojaZika);
				HIBERNATE_RESOURCE.commit();
	
				if (paramSeguimientoZika != "") {
	
					for (int i = 0; i < seguimientoZikaArray.size(); i++) {
						seguimientoZika = new SeguimientoZika();
						obj = new Object();
						obj = (Object) parser.parse(seguimientoZikaArray
								.get(i).toString());
						JSONObject seguimientoZikaJSON = (JSONObject) obj;
	
						sql = "select s from SeguimientoZika s where s.secHojaZika = :secHojaZika and s.controlDia = :controlDia";
						query = HIBERNATE_RESOURCE.getSession().createQuery(sql);
						query.setParameter("secHojaZika",
								hojaZika.getSecHojaZika());
						
						query.setParameter("controlDia", Integer.valueOf((String) seguimientoZikaJSON
								.get("controlDia"))) ;			
												
						if ((query.uniqueResult() != null))
							seguimientoZika = (SeguimientoZika) query
									.uniqueResult();
						
						seguimientoZika.setSecHojaZika(hojaZika.getSecHojaZika());
						
						seguimientoZika.setControlDia(Integer.valueOf((String) seguimientoZikaJSON
								.get("controlDia")));
						seguimientoZika.setFechaSeguimiento(df.parse(
								seguimientoZikaJSON.get("fechaSeguimiento").toString()));
						seguimientoZika.setUsuarioMedico(((Number) seguimientoZikaJSON.
								get("usuarioMedico")).shortValue());
						seguimientoZika.setSupervisor(((Number) seguimientoZikaJSON.
								get("supervisor")).shortValue());
						seguimientoZika
								.setConsultaInicial(((String) seguimientoZikaJSON
										.get("consultaInicial")));
						seguimientoZika.setFiebre(((String) seguimientoZikaJSON
								.get("fiebre")));
						seguimientoZika.setAstenia(((String) seguimientoZikaJSON
								.get("astenia")));
						seguimientoZika.setMalEstadoGral(((String) seguimientoZikaJSON
								.get("malEstadoGral")));
						seguimientoZika.setEscalosfrios(((String) seguimientoZikaJSON
								.get("escalosfrios")));
						seguimientoZika.setConvulsiones(((String) seguimientoZikaJSON
								.get("convulsiones")));
						seguimientoZika.setCefalea(((String) seguimientoZikaJSON
								.get("cefalea")));
						seguimientoZika.setRigidezCuello(((String) seguimientoZikaJSON
								.get("rigidezCuello")));
						seguimientoZika.setDolorRetroocular(((String) seguimientoZikaJSON
								.get("dolorRetroocular")));
						seguimientoZika.setPocoApetito(((String) seguimientoZikaJSON
								.get("pocoApetito")));
						seguimientoZika.setNauseas(((String) seguimientoZikaJSON
								.get("nauseas")));
						seguimientoZika.setVomitos(((String) seguimientoZikaJSON
								.get("vomitos")));
						seguimientoZika.setDiarrea(((String) seguimientoZikaJSON
								.get("diarrea")));
						seguimientoZika.setDolorAbdominalContinuo(((String) seguimientoZikaJSON
								.get("dolorAbdominalContinuo")));
						seguimientoZika.setArtralgiaProximal(((String) seguimientoZikaJSON
								.get("artralgiaProximal")));
						seguimientoZika.setArtralgiaDistal(((String) seguimientoZikaJSON
								.get("artralgiaDistal")));
						seguimientoZika.setMialgia(((String) seguimientoZikaJSON
								.get("mialgia")));
						seguimientoZika.setConjuntivitisNoPurulenta(((String) seguimientoZikaJSON
								.get("conjuntivitisNoPurulenta")));
						seguimientoZika.setEdemaArtProxMS(((String) seguimientoZikaJSON
								.get("edemaArtProxMS")));
						seguimientoZika.setEdemaArtDistMS(((String) seguimientoZikaJSON
								.get("edemaArtDistMS")));
						seguimientoZika.setEdemaArtProxMI(((String) seguimientoZikaJSON
								.get("edemaArtProxMI")));
						seguimientoZika.setEdemaArtDistMI(((String) seguimientoZikaJSON
								.get("edemaArtDistMI")));
						seguimientoZika.setEdemaPeriauricular(((String) seguimientoZikaJSON
								.get("edemaPeriauricular")));
						seguimientoZika.setAdenopatiaCervAnt(((String) seguimientoZikaJSON
								.get("adenopatiaCervAnt")));
						seguimientoZika.setAdenopatiaCervPost(((String) seguimientoZikaJSON
								.get("adenopatiaCervPost")));
						seguimientoZika.setAdenopatiaRetroAuricular(((String) seguimientoZikaJSON
								.get("adenopatiaRetroAuricular")));
						seguimientoZika.setRash(((String) seguimientoZikaJSON
								.get("rash")));
						seguimientoZika.setEquimosis(((String) seguimientoZikaJSON
								.get("equimosis")));
						seguimientoZika.setPruebaTorniquetePos(((String) seguimientoZikaJSON
								.get("pruebaTorniquetePos")));
						seguimientoZika.setEpistaxis(((String) seguimientoZikaJSON
								.get("epistaxis")));
						seguimientoZika.setGingivorragia(((String) seguimientoZikaJSON
								.get("gingivorragia")));
						seguimientoZika.setPetequiasEspontaneas(((String) seguimientoZikaJSON
								.get("petequiasEspontaneas")));
						seguimientoZika.setHematemesis(((String) seguimientoZikaJSON
								.get("hematemesis")));
						seguimientoZika.setMelena(((String) seguimientoZikaJSON
								.get("melena")));
						seguimientoZika.setOftalmoplejia(((String) seguimientoZikaJSON
								.get("oftalmoplejia")));
						seguimientoZika.setDificultadResp(((String) seguimientoZikaJSON
								.get("dificultadRespiratoria")));
						seguimientoZika.setDebilidadMuscMS(((String) seguimientoZikaJSON
								.get("debilidadMuscMS")));
						seguimientoZika.setDebilidadMuscMI(((String) seguimientoZikaJSON
								.get("debilidadMuscMI")));
						seguimientoZika.setParestesiaMS(((String) seguimientoZikaJSON
								.get("parestesiaMS")));
						seguimientoZika.setParestesiaMI(((String) seguimientoZikaJSON
								.get("parestesiaMI")));
						seguimientoZika.setParalisisMuscMS(((String) seguimientoZikaJSON
								.get("paralisisMuscMS")));
						seguimientoZika.setParalisisMuscMI(((String) seguimientoZikaJSON
								.get("paralisisMuscMI")));
						seguimientoZika.setTos(((String) seguimientoZikaJSON
								.get("tos")));
						seguimientoZika.setRinorrea(((String) seguimientoZikaJSON
								.get("rinorrea")));
						seguimientoZika.setDolorGarganta(((String) seguimientoZikaJSON
								.get("dolorGarganta")));
						seguimientoZika.setPrurito(((String) seguimientoZikaJSON
								.get("prurito")));
						
					
						HIBERNATE_RESOURCE.begin();
						HIBERNATE_RESOURCE.getSession().saveOrUpdate(
								seguimientoZika);
						HIBERNATE_RESOURCE.commit();
					}
				}
				List oLista = new LinkedList();
				Map fila = null;
				fila = new HashMap();
				fila.put("numHojaSeguimiento",
						hojaZika.getNumHojaSeguimiento());
				oLista.add(fila);
				result = UtilResultado.parserResultado(oLista, "", UtilResultado.OK);
			} else {
				result = UtilResultado.parserResultado(null, Mensajes.HOJA_INFLUENZA_CERRADA, UtilResultado.INFO);
			}
		} catch (Exception e) {
			e.printStackTrace();
			result = UtilResultado.parserResultado(null,
					Mensajes.ERROR_NO_CONTROLADO + e.getMessage(),
					UtilResultado.ERROR);
			HIBERNATE_RESOURCE.rollback();
			// TODO: handle exception
		} finally {
			if (HIBERNATE_RESOURCE.getSession().isOpen()) {
				HIBERNATE_RESOURCE.close();
			}
		}
		return result;
	}
	
	
	public byte[] getSeguimientoZikaPdf(int numHojaSeguimiento) {

		String nombreReporte = "rptSeguimientoZika";
		try {
			List oLista = new LinkedList(); // Listado final para el resultado

			String sql = " select distinct p.cod_expediente \"codExpediente\", p.nombre1, p.nombre2, p.apellido1, p.apellido2, "
					+ " h.num_hoja_seguimiento \"numHojaSeguimiento\", "
					+ " h.fis, h.fif, h.fecha_inicio \"fechaInicio\", "
					+ " h.fecha_cierre  \"fechaCierre\", "
					+ " h.categoria \"categoria\", "
					+ " h.sintoma_inicial1 \"sintomaInicial1\", "
					+ " h.sintoma_inicial2 \"sintomaInicial2\", "
					+ " h.sintoma_inicial3 \"sintomaInicial3\", "
					+ " h.sintoma_inicial4 \"sintomaInicial4\", "
					
					+ " s1.consulta_inicial \"consultaInicialDia1\", "
					+ " s2.consulta_inicial \"consultaInicialDia2\", "
					+ " s3.consulta_inicial \"consultaInicialDia3\", "
					+ " s4.consulta_inicial \"consultaInicialDia4\", "
					+ " s5.consulta_inicial \"consultaInicialDia5\", "
					+ " s6.consulta_inicial \"consultaInicialDia6\", "
					+ " s7.consulta_inicial \"consultaInicialDia7\", "
					+ " s8.consulta_inicial \"consultaInicialDia8\", "
					+ " s9.consulta_inicial \"consultaInicialDia9\", "
					+ " s10.consulta_inicial \"consultaInicialDia10\", "
					+ " s11.consulta_inicial \"consultaInicialDia11\", "
					+ " s12.consulta_inicial \"consultaInicialDia12\", "
					+ " s13.consulta_inicial \"consultaInicialDia13\", "
					+ " s14.consulta_inicial \"consultaInicialDia14\", "
					
					+ " s1.fiebre \"fiebreDia1\", "
					+ " s2.fiebre \"fiebreDia2\", "
					+ " s3.fiebre \"fiebreDia3\", "
					+ " s4.fiebre \"fiebreDia4\", "
					+ " s5.fiebre \"fiebreDia5\", "
					+ " s6.fiebre \"fiebreDia6\", "
					+ " s7.fiebre \"fiebreDia7\", "
					+ " s8.fiebre \"fiebreDia8\", "
					+ " s9.fiebre \"fiebreDia9\", "
					+ " s10.fiebre \"fiebreDia10\", "
					+ " s11.fiebre \"fiebreDia11\", "
					+ " s12.fiebre \"fiebreDia12\", "
					+ " s13.fiebre \"fiebreDia13\", "
					+ " s14.fiebre \"fiebreDia14\", "
					
					+ " s1.astenia \"asteniaDia1\", "
					+ " s2.astenia \"asteniaDia2\", "
					+ " s3.astenia \"asteniaDia3\", "
					+ " s4.astenia \"asteniaDia4\", "
					+ " s5.astenia \"asteniaDia5\", "
					+ " s6.astenia \"asteniaDia6\", "
					+ " s7.astenia \"asteniaDia7\", "
					+ " s8.astenia \"asteniaDia8\", "
					+ " s9.astenia \"asteniaDia9\", "
					+ " s10.astenia \"asteniaDia10\", "
					+ " s11.astenia \"asteniaDia11\", "
					+ " s12.astenia \"asteniaDia12\", "
					+ " s13.astenia \"asteniaDia13\", "
					+ " s14.astenia \"asteniaDia14\", "
					
					+ " s1.mal_estado_gral \"malEstadoGralDia1\", "
					+ " s2.mal_estado_gral \"malEstadoGralDia2\", "
					+ " s3.mal_estado_gral \"malEstadoGralDia3\", "
					+ " s4.mal_estado_gral \"malEstadoGralDia4\", "
					+ " s5.mal_estado_gral \"malEstadoGralDia5\", "
					+ " s6.mal_estado_gral \"malEstadoGralDia6\", "
					+ " s7.mal_estado_gral \"malEstadoGralDia7\", "
					+ " s8.mal_estado_gral \"malEstadoGralDia8\", "
					+ " s9.mal_estado_gral \"malEstadoGralDia9\", "
					+ " s10.mal_estado_gral \"malEstadoGralDia10\", "
					+ " s11.mal_estado_gral \"malEstadoGralDia11\", "
					+ " s12.mal_estado_gral \"malEstadoGralDia12\", "
					+ " s13.mal_estado_gral \"malEstadoGralDia13\", "
					+ " s14.mal_estado_gral \"malEstadoGralDia14\", "
					
					+ " s1.escalosfrios \"escalosfriosDia1\", "
					+ " s2.escalosfrios \"escalosfriosDia2\", "
					+ " s3.escalosfrios \"escalosfriosDia3\", "
					+ " s4.escalosfrios \"escalosfriosDia4\", "
					+ " s5.escalosfrios \"escalosfriosDia5\", "
					+ " s6.escalosfrios \"escalosfriosDia6\", "
					+ " s7.escalosfrios \"escalosfriosDia7\", "
					+ " s8.escalosfrios \"escalosfriosDia8\", "
					+ " s9.escalosfrios \"escalosfriosDia9\", "
					+ " s10.escalosfrios \"escalosfriosDia10\", "
					+ " s11.escalosfrios \"escalosfriosDia11\", "
					+ " s12.escalosfrios \"escalosfriosDia12\", "
					+ " s13.escalosfrios \"escalosfriosDia13\", "
					+ " s14.escalosfrios \"escalosfriosDia14\", "
					
					+ " s1.convulsiones \"convulsionesDia1\", "
					+ " s2.convulsiones \"convulsionesDia2\", "
					+ " s3.convulsiones \"convulsionesDia3\", "
					+ " s4.convulsiones \"convulsionesDia4\", "
					+ " s5.convulsiones \"convulsionesDia5\", "
					+ " s6.convulsiones \"convulsionesDia6\", "
					+ " s7.convulsiones \"convulsionesDia7\", "
					+ " s8.convulsiones \"convulsionesDia8\", "
					+ " s9.convulsiones \"convulsionesDia9\", "
					+ " s10.convulsiones \"convulsionesDia10\", "
					+ " s11.convulsiones \"convulsionesDia11\", "
					+ " s12.convulsiones \"convulsionesDia12\", "
					+ " s13.convulsiones \"convulsionesDia13\", "
					+ " s14.convulsiones \"convulsionesDia14\", "
					
					+ " s1.cefalea \"cefaleaDia1\", "
					+ " s2.cefalea \"cefaleaDia2\", "
					+ " s3.cefalea \"cefaleaDia3\", "
					+ " s4.cefalea \"cefaleaDia4\", "
					+ " s5.cefalea \"cefaleaDia5\", "
					+ " s6.cefalea \"cefaleaDia6\", "
					+ " s7.cefalea \"cefaleaDia7\", "
					+ " s8.cefalea \"cefaleaDia8\", "
					+ " s9.cefalea \"cefaleaDia9\", "
					+ " s10.cefalea \"cefaleaDia10\", "
					+ " s11.cefalea \"cefaleaDia11\", "
					+ " s12.cefalea \"cefaleaDia12\", "
					+ " s13.cefalea \"cefaleaDia13\", "
					+ " s14.cefalea \"cefaleaDia14\", "
					
					+ " s1.rigidez_cuello \"rigidezCuelloDia1\", "
					+ " s2.rigidez_cuello \"rigidezCuelloDia2\", "
					+ " s3.rigidez_cuello \"rigidezCuelloDia3\", "
					+ " s4.rigidez_cuello \"rigidezCuelloDia4\", "
					+ " s5.rigidez_cuello \"rigidezCuelloDia5\", "
					+ " s6.rigidez_cuello \"rigidezCuelloDia6\", "
					+ " s7.rigidez_cuello \"rigidezCuelloDia7\", "
					+ " s8.rigidez_cuello \"rigidezCuelloDia8\", "
					+ " s9.rigidez_cuello \"rigidezCuelloDia9\", "
					+ " s10.rigidez_cuello \"rigidezCuelloDia10\", "
					+ " s11.rigidez_cuello \"rigidezCuelloDia11\", "
					+ " s12.rigidez_cuello \"rigidezCuelloDia12\", "
					+ " s13.rigidez_cuello \"rigidezCuelloDia13\", "
					+ " s14.rigidez_cuello \"rigidezCuelloDia14\", "
					
					+ " s1.dolor_retroocular \"dolorRetroocularDia1\", "
					+ " s2.dolor_retroocular \"dolorRetroocularDia2\", "
					+ " s3.dolor_retroocular \"dolorRetroocularDia3\", "
					+ " s4.dolor_retroocular \"dolorRetroocularDia4\", "
					+ " s5.dolor_retroocular \"dolorRetroocularDia5\", "
					+ " s6.dolor_retroocular \"dolorRetroocularDia6\", "
					+ " s7.dolor_retroocular \"dolorRetroocularDia7\", "
					+ " s8.dolor_retroocular \"dolorRetroocularDia8\", "
					+ " s9.dolor_retroocular \"dolorRetroocularDia9\", "
					+ " s10.dolor_retroocular \"dolorRetroocularDia10\", "
					+ " s11.dolor_retroocular \"dolorRetroocularDia11\", "
					+ " s12.dolor_retroocular \"dolorRetroocularDia12\", "
					+ " s13.dolor_retroocular \"dolorRetroocularDia13\", "
					+ " s14.dolor_retroocular \"dolorRetroocularDia14\", "
					
					+ " s1.poco_apetito \"pocoApetitoDia1\", "
					+ " s2.poco_apetito \"pocoApetitoDia2\", "
					+ " s3.poco_apetito \"pocoApetitoDia3\", "
					+ " s4.poco_apetito \"pocoApetitoDia4\", "
					+ " s5.poco_apetito \"pocoApetitoDia5\", "
					+ " s6.poco_apetito \"pocoApetitoDia6\", "
					+ " s7.poco_apetito \"pocoApetitoDia7\", "
					+ " s8.poco_apetito \"pocoApetitoDia8\", "
					+ " s9.poco_apetito \"pocoApetitoDia9\", "
					+ " s10.poco_apetito \"pocoApetitoDia10\", "
					+ " s11.poco_apetito \"pocoApetitoDia11\", "
					+ " s12.poco_apetito \"pocoApetitoDia12\", "
					+ " s13.poco_apetito \"pocoApetitoDia13\", "
					+ " s14.poco_apetito \"pocoApetitoDia14\", "
					
					+ " s1.nauseas \"nauseasDia1\", "
					+ " s2.nauseas \"nauseasDia2\", "
					+ " s3.nauseas \"nauseasDia3\", "
					+ " s4.nauseas \"nauseasDia4\", "
					+ " s5.nauseas \"nauseasDia5\", "
					+ " s6.nauseas \"nauseasDia6\", "
					+ " s7.nauseas \"nauseasDia7\", "
					+ " s8.nauseas \"nauseasDia8\", "
					+ " s9.nauseas \"nauseasDia9\", "
					+ " s10.nauseas \"nauseasDia10\", "
					+ " s11.nauseas \"nauseasDia11\", "
					+ " s12.nauseas \"nauseasDia12\", "
					+ " s13.nauseas \"nauseasDia13\", "
					+ " s14.nauseas \"nauseasDia14\", "
					
					+ " s1.vomitos \"vomitosDia1\", "
					+ " s2.vomitos \"vomitosDia2\", "
					+ " s3.vomitos \"vomitosDia3\", "
					+ " s4.vomitos \"vomitosDia4\", "
					+ " s5.vomitos \"vomitosDia5\", "
					+ " s6.vomitos \"vomitosDia6\", "
					+ " s7.vomitos \"vomitosDia7\", "
					+ " s8.vomitos \"vomitosDia8\", "
					+ " s9.vomitos \"vomitosDia9\", "
					+ " s10.vomitos \"vomitosDia10\", "
					+ " s11.vomitos \"vomitosDia11\", "
					+ " s12.vomitos \"vomitosDia12\", "
					+ " s13.vomitos \"vomitosDia13\", "
					+ " s14.vomitos \"vomitosDia14\", "
					
					+ " s1.diarrea \"diarreaDia1\", "
					+ " s2.diarrea \"diarreaDia2\", "
					+ " s3.diarrea \"diarreaDia3\", "
					+ " s4.diarrea \"diarreaDia4\", "
					+ " s5.diarrea \"diarreaDia5\", "
					+ " s6.diarrea \"diarreaDia6\", "
					+ " s7.diarrea \"diarreaDia7\", "
					+ " s8.diarrea \"diarreaDia8\", "
					+ " s9.diarrea \"diarreaDia9\", "
					+ " s10.diarrea \"diarreaDia10\", "
					+ " s11.diarrea \"diarreaDia11\", "
					+ " s12.diarrea \"diarreaDia12\", "
					+ " s13.diarrea \"diarreaDia13\", "
					+ " s14.diarrea \"diarreaDia14\", "
					
					+ " s1.dolor_abdominal_continuo \"dolorAbdominalContinuoDia1\", "
					+ " s2.dolor_abdominal_continuo \"dolorAbdominalContinuoDia2\", "
					+ " s3.dolor_abdominal_continuo \"dolorAbdominalContinuoDia3\", "
					+ " s4.dolor_abdominal_continuo \"dolorAbdominalContinuoDia4\", "
					+ " s5.dolor_abdominal_continuo \"dolorAbdominalContinuoDia5\", "
					+ " s6.dolor_abdominal_continuo \"dolorAbdominalContinuoDia6\", "
					+ " s7.dolor_abdominal_continuo \"dolorAbdominalContinuoDia7\", "
					+ " s8.dolor_abdominal_continuo \"dolorAbdominalContinuoDia8\", "
					+ " s9.dolor_abdominal_continuo \"dolorAbdominalContinuoDia9\", "
					+ " s10.dolor_abdominal_continuo \"dolorAbdominalContinuoDia10\", "
					+ " s11.dolor_abdominal_continuo \"dolorAbdominalContinuoDia11\", "
					+ " s12.dolor_abdominal_continuo \"dolorAbdominalContinuoDia12\", "
					+ " s13.dolor_abdominal_continuo \"dolorAbdominalContinuoDia13\", "
					+ " s14.dolor_abdominal_continuo \"dolorAbdominalContinuoDia14\", "
					
					+ " s1.artralgia_proximal \"artralgiaProximalDia1\", "
					+ " s2.artralgia_proximal \"artralgiaProximalDia2\", "
					+ " s3.artralgia_proximal \"artralgiaProximalDia3\", "
					+ " s4.artralgia_proximal \"artralgiaProximalDia4\", "
					+ " s5.artralgia_proximal \"artralgiaProximalDia5\", "
					+ " s6.artralgia_proximal \"artralgiaProximalDia6\", "
					+ " s7.artralgia_proximal \"artralgiaProximalDia7\", "
					+ " s8.artralgia_proximal \"artralgiaProximalDia8\", "
					+ " s9.artralgia_proximal \"artralgiaProximalDia9\", "
					+ " s10.artralgia_proximal \"artralgiaProximalDia10\", "
					+ " s11.artralgia_proximal \"artralgiaProximalDia11\", "
					+ " s12.artralgia_proximal \"artralgiaProximalDia12\", "
					+ " s13.artralgia_proximal \"artralgiaProximalDia13\", "
					+ " s14.artralgia_proximal \"artralgiaProximalDia14\", "
					
					+ " s1.artralgia_distal \"artralgiaDistalDia1\", "
					+ " s2.artralgia_distal \"artralgiaDistalDia2\", "
					+ " s3.artralgia_distal \"artralgiaDistalDia3\", "
					+ " s4.artralgia_distal \"artralgiaDistalDia4\", "
					+ " s5.artralgia_distal \"artralgiaDistalDia5\", "
					+ " s6.artralgia_distal \"artralgiaDistalDia6\", "
					+ " s7.artralgia_distal \"artralgiaDistalDia7\", "
					+ " s8.artralgia_distal \"artralgiaDistalDia8\", "
					+ " s9.artralgia_distal \"artralgiaDistalDia9\", "
					+ " s10.artralgia_distal \"artralgiaDistalDia10\", "
					+ " s11.artralgia_distal \"artralgiaDistalDia11\", "
					+ " s12.artralgia_distal \"artralgiaDistalDia12\", "
					+ " s13.artralgia_distal \"artralgiaDistalDia13\", "
					+ " s14.artralgia_distal \"artralgiaDistalDia14\", "
					
					+ " s1.mialgia \"mialgiaDia1\", "
					+ " s2.mialgia \"mialgiaDia2\", "
					+ " s3.mialgia \"mialgiaDia3\", "
					+ " s4.mialgia \"mialgiaDia4\", "
					+ " s5.mialgia \"mialgiaDia5\", "
					+ " s6.mialgia \"mialgiaDia6\", "
					+ " s7.mialgia \"mialgiaDia7\", "
					+ " s8.mialgia \"mialgiaDia8\", "
					+ " s9.mialgia \"mialgiaDia9\", "
					+ " s10.mialgia \"mialgiaDia10\", "
					+ " s11.mialgia \"mialgiaDia11\", "
					+ " s12.mialgia \"mialgiaDia12\", "
					+ " s13.mialgia \"mialgiaDia13\", "
					+ " s14.mialgia \"mialgiaDia14\", "
					
					+ " s1.conjuntivitis_nopurulenta \"conjuntivitisNoPurulentaDia1\", "
					+ " s2.conjuntivitis_nopurulenta \"conjuntivitisNoPurulentaDia2\", "
					+ " s3.conjuntivitis_nopurulenta \"conjuntivitisNoPurulentaDia3\", "
					+ " s4.conjuntivitis_nopurulenta \"conjuntivitisNoPurulentaDia4\", "
					+ " s5.conjuntivitis_nopurulenta \"conjuntivitisNoPurulentaDia5\", "
					+ " s6.conjuntivitis_nopurulenta \"conjuntivitisNoPurulentaDia6\", "
					+ " s7.conjuntivitis_nopurulenta \"conjuntivitisNoPurulentaDia7\", "
					+ " s8.conjuntivitis_nopurulenta \"conjuntivitisNoPurulentaDia8\", "
					+ " s9.conjuntivitis_nopurulenta \"conjuntivitisNoPurulentaDia9\", "
					+ " s10.conjuntivitis_nopurulenta \"conjuntivitisNoPurulentaDia10\", "
					+ " s11.conjuntivitis_nopurulenta \"conjuntivitisNoPurulentaDia11\", "
					+ " s12.conjuntivitis_nopurulenta \"conjuntivitisNoPurulentaDia12\", "
					+ " s13.conjuntivitis_nopurulenta \"conjuntivitisNoPurulentaDia13\", "
					+ " s14.conjuntivitis_nopurulenta \"conjuntivitisNoPurulentaDia14\", "
					
					+ " s1.edema_art_prox_ms \"edemaArtProxMSDia1\", "
					+ " s2.edema_art_prox_ms \"edemaArtProxMSDia2\", "
					+ " s3.edema_art_prox_ms \"edemaArtProxMSDia3\", "
					+ " s4.edema_art_prox_ms \"edemaArtProxMSDia4\", "
					+ " s5.edema_art_prox_ms \"edemaArtProxMSDia5\", "
					+ " s6.edema_art_prox_ms \"edemaArtProxMSDia6\", "
					+ " s7.edema_art_prox_ms \"edemaArtProxMSDia7\", "
					+ " s8.edema_art_prox_ms \"edemaArtProxMSDia8\", "
					+ " s9.edema_art_prox_ms \"edemaArtProxMSDia9\", "
					+ " s10.edema_art_prox_ms \"edemaArtProxMSDia10\", "
					+ " s11.edema_art_prox_ms \"edemaArtProxMSDia11\", "
					+ " s12.edema_art_prox_ms \"edemaArtProxMSDia12\", "
					+ " s13.edema_art_prox_ms \"edemaArtProxMSDia13\", "
					+ " s14.edema_art_prox_ms \"edemaArtProxMSDia14\", "
					
					+ " s1.edema_art_dist_ms \"edemaArtDistMSDia1\", "
					+ " s2.edema_art_dist_ms \"edemaArtDistMSDia2\", "
					+ " s3.edema_art_dist_ms \"edemaArtDistMSDia3\", "
					+ " s4.edema_art_dist_ms \"edemaArtDistMSDia4\", "
					+ " s5.edema_art_dist_ms \"edemaArtDistMSDia5\", "
					+ " s6.edema_art_dist_ms \"edemaArtDistMSDia6\", "
					+ " s7.edema_art_dist_ms \"edemaArtDistMSDia7\", "
					+ " s8.edema_art_dist_ms \"edemaArtDistMSDia8\", "
					+ " s9.edema_art_dist_ms \"edemaArtDistMSDia9\", "
					+ " s10.edema_art_dist_ms \"edemaArtDistMSDia10\", "
					+ " s11.edema_art_dist_ms \"edemaArtDistMSDia11\", "
					+ " s12.edema_art_dist_ms \"edemaArtDistMSDia12\", "
					+ " s13.edema_art_dist_ms \"edemaArtDistMSDia13\", "
					+ " s14.edema_art_dist_ms \"edemaArtDistMSDia14\", "
					
					+ " s1.edema_art_prox_mi \"edemaArtProxMIDia1\", "
					+ " s2.edema_art_prox_mi \"edemaArtProxMIDia2\", "
					+ " s3.edema_art_prox_mi \"edemaArtProxMIDia3\", "
					+ " s4.edema_art_prox_mi \"edemaArtProxMIDia4\", "
					+ " s5.edema_art_prox_mi \"edemaArtProxMIDia5\", "
					+ " s6.edema_art_prox_mi \"edemaArtProxMIDia6\", "
					+ " s7.edema_art_prox_mi \"edemaArtProxMIDia7\", "
					+ " s8.edema_art_prox_mi \"edemaArtProxMIDia8\", "
					+ " s9.edema_art_prox_mi \"edemaArtProxMIDia9\", "
					+ " s10.edema_art_prox_mi \"edemaArtProxMIDia10\", "
					+ " s11.edema_art_prox_mi \"edemaArtProxMIDia11\", "
					+ " s12.edema_art_prox_mi \"edemaArtProxMIDia12\", "
					+ " s13.edema_art_prox_mi \"edemaArtProxMIDia13\", "
					+ " s14.edema_art_prox_mi \"edemaArtProxMIDia14\", "
					
					+ " s1.edema_art_dist_mi \"edemaArtDistMIDia1\", "
					+ " s2.edema_art_dist_mi \"edemaArtDistMIDia2\", "
					+ " s3.edema_art_dist_mi \"edemaArtDistMIDia3\", "
					+ " s4.edema_art_dist_mi \"edemaArtDistMIDia4\", "
					+ " s5.edema_art_dist_mi \"edemaArtDistMIDia5\", "
					+ " s6.edema_art_dist_mi \"edemaArtDistMIDia6\", "
					+ " s7.edema_art_dist_mi \"edemaArtDistMIDia7\", "
					+ " s8.edema_art_dist_mi \"edemaArtDistMIDia8\", "
					+ " s9.edema_art_dist_mi \"edemaArtDistMIDia9\", "
					+ " s10.edema_art_dist_mi \"edemaArtDistMIDia10\", "
					+ " s11.edema_art_dist_mi \"edemaArtDistMIDia11\", "
					+ " s12.edema_art_dist_mi \"edemaArtDistMIDia12\", "
					+ " s13.edema_art_dist_mi \"edemaArtDistMIDia13\", "
					+ " s14.edema_art_dist_mi \"edemaArtDistMIDia14\", "
					
					+ " s1.edema_periauricular \"edemaPeriauricularDia1\", "
					+ " s2.edema_periauricular \"edemaPeriauricularDia2\", "
					+ " s3.edema_periauricular \"edemaPeriauricularDia3\", "
					+ " s4.edema_periauricular \"edemaPeriauricularDia4\", "
					+ " s5.edema_periauricular \"edemaPeriauricularDia5\", "
					+ " s6.edema_periauricular \"edemaPeriauricularDia6\", "
					+ " s7.edema_periauricular \"edemaPeriauricularDia7\", "
					+ " s8.edema_periauricular \"edemaPeriauricularDia8\", "
					+ " s9.edema_periauricular \"edemaPeriauricularDia9\", "
					+ " s10.edema_periauricular \"edemaPeriauricularDia10\", "
					+ " s11.edema_periauricular \"edemaPeriauricularDia11\", "
					+ " s12.edema_periauricular \"edemaPeriauricularDia12\", "
					+ " s13.edema_periauricular \"edemaPeriauricularDia13\", "
					+ " s14.edema_periauricular \"edemaPeriauricularDia14\", "
					
					+ " s1.adenopatia_cerv_ant \"adenopatiaCervAntDia1\", "
					+ " s2.adenopatia_cerv_ant \"adenopatiaCervAntDia2\", "
					+ " s3.adenopatia_cerv_ant \"adenopatiaCervAntDia3\", "
					+ " s4.adenopatia_cerv_ant \"adenopatiaCervAntDia4\", "
					+ " s5.adenopatia_cerv_ant \"adenopatiaCervAntDia5\", "
					+ " s6.adenopatia_cerv_ant \"adenopatiaCervAntDia6\", "
					+ " s7.adenopatia_cerv_ant \"adenopatiaCervAntDia7\", "
					+ " s8.adenopatia_cerv_ant \"adenopatiaCervAntDia8\", "
					+ " s9.adenopatia_cerv_ant \"adenopatiaCervAntDia9\", "
					+ " s10.adenopatia_cerv_ant \"adenopatiaCervAntDia10\", "
					+ " s11.adenopatia_cerv_ant \"adenopatiaCervAntDia11\", "
					+ " s12.adenopatia_cerv_ant \"adenopatiaCervAntDia12\", "
					+ " s13.adenopatia_cerv_ant \"adenopatiaCervAntDia13\", "
					+ " s14.adenopatia_cerv_ant \"adenopatiaCervAntDia14\", "
					
					+ " s1.adenopatia_cerv_post \"adenopatiaCervPostDia1\", "
					+ " s2.adenopatia_cerv_post \"adenopatiaCervPostDia2\", "
					+ " s3.adenopatia_cerv_post \"adenopatiaCervPostDia3\", "
					+ " s4.adenopatia_cerv_post \"adenopatiaCervPostDia4\", "
					+ " s5.adenopatia_cerv_post \"adenopatiaCervPostDia5\", "
					+ " s6.adenopatia_cerv_post \"adenopatiaCervPostDia6\", "
					+ " s7.adenopatia_cerv_post \"adenopatiaCervPostDia7\", "
					+ " s8.adenopatia_cerv_post \"adenopatiaCervPostDia8\", "
					+ " s9.adenopatia_cerv_post \"adenopatiaCervPostDia9\", "
					+ " s10.adenopatia_cerv_post \"adenopatiaCervPostDia10\", "
					+ " s11.adenopatia_cerv_post \"adenopatiaCervPostDia11\", "
					+ " s12.adenopatia_cerv_post \"adenopatiaCervPostDia12\", "
					+ " s13.adenopatia_cerv_post \"adenopatiaCervPostDia13\", "
					+ " s14.adenopatia_cerv_post \"adenopatiaCervPostDia14\", "
					
					+ " s1.adenopatia_retro_auricular \"adenopatiaRetroAuricularDia1\", "
					+ " s2.adenopatia_retro_auricular \"adenopatiaRetroAuricularDia2\", "
					+ " s3.adenopatia_retro_auricular \"adenopatiaRetroAuricularDia3\", "
					+ " s4.adenopatia_retro_auricular \"adenopatiaRetroAuricularDia4\", "
					+ " s5.adenopatia_retro_auricular \"adenopatiaRetroAuricularDia5\", "
					+ " s6.adenopatia_retro_auricular \"adenopatiaRetroAuricularDia6\", "
					+ " s7.adenopatia_retro_auricular \"adenopatiaRetroAuricularDia7\", "
					+ " s8.adenopatia_retro_auricular \"adenopatiaRetroAuricularDia8\", "
					+ " s9.adenopatia_retro_auricular \"adenopatiaRetroAuricularDia9\", "
					+ " s10.adenopatia_retro_auricular \"adenopatiaRetroAuricularDia10\", "
					+ " s11.adenopatia_retro_auricular \"adenopatiaRetroAuricularDia11\", "
					+ " s12.adenopatia_retro_auricular \"adenopatiaRetroAuricularDia12\", "
					+ " s13.adenopatia_retro_auricular \"adenopatiaRetroAuricularDia13\", "
					+ " s14.adenopatia_retro_auricular \"adenopatiaRetroAuricularDia14\", "
					
					+ " s1.rash \"rashDia1\", "
					+ " s2.rash \"rashDia2\", "
					+ " s3.rash \"rashDia3\", "
					+ " s4.rash \"rashDia4\", "
					+ " s5.rash \"rashDia5\", "
					+ " s6.rash \"rashDia6\", "
					+ " s7.rash \"rashDia7\", "
					+ " s8.rash \"rashDia8\", "
					+ " s9.rash \"rashDia9\", "
					+ " s10.rash \"rashDia10\", "
					+ " s11.rash \"rashDia11\", "
					+ " s12.rash \"rashDia12\", "
					+ " s13.rash \"rashDia13\", "
					+ " s14.rash \"rashDia14\", "
					
					+ " s1.equimosis \"equimosisDia1\", "
					+ " s2.equimosis \"equimosisDia2\", "
					+ " s3.equimosis \"equimosisDia3\", "
					+ " s4.equimosis \"equimosisDia4\", "
					+ " s5.equimosis \"equimosisDia5\", "
					+ " s6.equimosis \"equimosisDia6\", "
					+ " s7.equimosis \"equimosisDia7\", "
					+ " s8.equimosis \"equimosisDia8\", "
					+ " s9.equimosis \"equimosisDia9\", "
					+ " s10.equimosis \"equimosisDia10\", "
					+ " s11.equimosis \"equimosisDia11\", "
					+ " s12.equimosis \"equimosisDia12\", "
					+ " s13.equimosis \"equimosisDia13\", "
					+ " s14.equimosis \"equimosisDia14\", "
					
					+ " s1.prueba_torniquete_pos \"pruebaTorniquetePosDia1\", "
					+ " s2.prueba_torniquete_pos \"pruebaTorniquetePosDia2\", "
					+ " s3.prueba_torniquete_pos \"pruebaTorniquetePosDia3\", "
					+ " s4.prueba_torniquete_pos \"pruebaTorniquetePosDia4\", "
					+ " s5.prueba_torniquete_pos \"pruebaTorniquetePosDia5\", "
					+ " s6.prueba_torniquete_pos \"pruebaTorniquetePosDia6\", "
					+ " s7.prueba_torniquete_pos \"pruebaTorniquetePosDia7\", "
					+ " s8.prueba_torniquete_pos \"pruebaTorniquetePosDia8\", "
					+ " s9.prueba_torniquete_pos \"pruebaTorniquetePosDia9\", "
					+ " s10.prueba_torniquete_pos \"pruebaTorniquetePosDia10\", "
					+ " s11.prueba_torniquete_pos \"pruebaTorniquetePosDia11\", "
					+ " s12.prueba_torniquete_pos \"pruebaTorniquetePosDia12\", "
					+ " s13.prueba_torniquete_pos \"pruebaTorniquetePosDia13\", "
					+ " s14.prueba_torniquete_pos \"pruebaTorniquetePosDia14\", "
					
					+ " s1.epistaxis \"epistaxisDia1\", "
					+ " s2.epistaxis \"epistaxisDia2\", "
					+ " s3.epistaxis \"epistaxisDia3\", "
					+ " s4.epistaxis \"epistaxisDia4\", "
					+ " s5.epistaxis \"epistaxisDia5\", "
					+ " s6.epistaxis \"epistaxisDia6\", "
					+ " s7.epistaxis \"epistaxisDia7\", "
					+ " s8.epistaxis \"epistaxisDia8\", "
					+ " s9.epistaxis \"epistaxisDia9\", "
					+ " s10.epistaxis \"epistaxisDia10\", "
					+ " s11.epistaxis \"epistaxisDia11\", "
					+ " s12.epistaxis \"epistaxisDia12\", "
					+ " s13.epistaxis \"epistaxisDia13\", "
					+ " s14.epistaxis \"epistaxisDia14\", "
					
					+ " s1.gingivorragia \"gingivorragiaDia1\", "
					+ " s2.gingivorragia \"gingivorragiaDia2\", "
					+ " s3.gingivorragia \"gingivorragiaDia3\", "
					+ " s4.gingivorragia \"gingivorragiaDia4\", "
					+ " s5.gingivorragia \"gingivorragiaDia5\", "
					+ " s6.gingivorragia \"gingivorragiaDia6\", "
					+ " s7.gingivorragia \"gingivorragiaDia7\", "
					+ " s8.gingivorragia \"gingivorragiaDia8\", "
					+ " s9.gingivorragia \"gingivorragiaDia9\", "
					+ " s10.gingivorragia \"gingivorragiaDia10\", "
					+ " s11.gingivorragia \"gingivorragiaDia11\", "
					+ " s12.gingivorragia \"gingivorragiaDia12\", "
					+ " s13.gingivorragia \"gingivorragiaDia13\", "
					+ " s14.gingivorragia \"gingivorragiaDia14\", "
					
					+ " s1.petequias_espontaneas \"petequiasEspontaneasDia1\", "
					+ " s2.petequias_espontaneas \"petequiasEspontaneasDia2\", "
					+ " s3.petequias_espontaneas \"petequiasEspontaneasDia3\", "
					+ " s4.petequias_espontaneas \"petequiasEspontaneasDia4\", "
					+ " s5.petequias_espontaneas \"petequiasEspontaneasDia5\", "
					+ " s6.petequias_espontaneas \"petequiasEspontaneasDia6\", "
					+ " s7.petequias_espontaneas \"petequiasEspontaneasDia7\", "
					+ " s8.petequias_espontaneas \"petequiasEspontaneasDia8\", "
					+ " s9.petequias_espontaneas \"petequiasEspontaneasDia9\", "
					+ " s10.petequias_espontaneas \"petequiasEspontaneasDia10\", "
					+ " s11.petequias_espontaneas \"petequiasEspontaneasDia11\", "
					+ " s12.petequias_espontaneas \"petequiasEspontaneasDia12\", "
					+ " s13.petequias_espontaneas \"petequiasEspontaneasDia13\", "
					+ " s14.petequias_espontaneas \"petequiasEspontaneasDia14\", "
					
					+ " s1.hematemesis \"hematemesisDia1\", "
					+ " s2.hematemesis \"hematemesisDia2\", "
					+ " s3.hematemesis \"hematemesisDia3\", "
					+ " s4.hematemesis \"hematemesisDia4\", "
					+ " s5.hematemesis \"hematemesisDia5\", "
					+ " s6.hematemesis \"hematemesisDia6\", "
					+ " s7.hematemesis \"hematemesisDia7\", "
					+ " s8.hematemesis \"hematemesisDia8\", "
					+ " s9.hematemesis \"hematemesisDia9\", "
					+ " s10.hematemesis \"hematemesisDia10\", "
					+ " s11.hematemesis \"hematemesisDia11\", "
					+ " s12.hematemesis \"hematemesisDia12\", "
					+ " s13.hematemesis \"hematemesisDia13\", "
					+ " s14.hematemesis \"hematemesisDia14\", "
					
					+ " s1.melena \"melenaDia1\", "
					+ " s2.melena \"melenaDia2\", "
					+ " s3.melena \"melenaDia3\", "
					+ " s4.melena \"melenaDia4\", "
					+ " s5.melena \"melenaDia5\", "
					+ " s6.melena \"melenaDia6\", "
					+ " s7.melena \"melenaDia7\", "
					+ " s8.melena \"melenaDia8\", "
					+ " s9.melena \"melenaDia9\", "
					+ " s10.melena \"melenaDia10\", "
					+ " s11.melena \"melenaDia11\", "
					+ " s12.melena \"melenaDia12\", "
					+ " s13.melena \"melenaDia13\", "
					+ " s14.melena \"melenaDia14\", "
					
					+ " s1.oftalmoplejia \"oftalmoplejiaDia1\", "
					+ " s2.oftalmoplejia \"oftalmoplejiaDia2\", "
					+ " s3.oftalmoplejia \"oftalmoplejiaDia3\", "
					+ " s4.oftalmoplejia \"oftalmoplejiaDia4\", "
					+ " s5.oftalmoplejia \"oftalmoplejiaDia5\", "
					+ " s6.oftalmoplejia \"oftalmoplejiaDia6\", "
					+ " s7.oftalmoplejia \"oftalmoplejiaDia7\", "
					+ " s8.oftalmoplejia \"oftalmoplejiaDia8\", "
					+ " s9.oftalmoplejia \"oftalmoplejiaDia9\", "
					+ " s10.oftalmoplejia \"oftalmoplejiaDia10\", "
					+ " s11.oftalmoplejia \"oftalmoplejiaDia11\", "
					+ " s12.oftalmoplejia \"oftalmoplejiaDia12\", "
					+ " s13.oftalmoplejia \"oftalmoplejiaDia13\", "
					+ " s14.oftalmoplejia \"oftalmoplejiaDia14\", "
					
					+ " s1.dificultad_respiratoria \"dificultadRespDia1\", "
					+ " s2.dificultad_respiratoria \"dificultadRespDia2\", "
					+ " s3.dificultad_respiratoria \"dificultadRespDia3\", "
					+ " s4.dificultad_respiratoria \"dificultadRespDia4\", "
					+ " s5.dificultad_respiratoria \"dificultadRespDia5\", "
					+ " s6.dificultad_respiratoria \"dificultadRespDia6\", "
					+ " s7.dificultad_respiratoria \"dificultadRespDia7\", "
					+ " s8.dificultad_respiratoria \"dificultadRespDia8\", "
					+ " s9.dificultad_respiratoria \"dificultadRespDia9\", "
					+ " s10.dificultad_respiratoria \"dificultadRespDia10\", "
					+ " s11.dificultad_respiratoria \"dificultadRespDia11\", "
					+ " s12.dificultad_respiratoria \"dificultadRespDia12\", "
					+ " s13.dificultad_respiratoria \"dificultadRespDia13\", "
					+ " s14.dificultad_respiratoria \"dificultadRespDia14\", "
					
					+ " s1.debilidad_muscular_ms \"debilidadMuscMSDia1\", "
					+ " s2.debilidad_muscular_ms \"debilidadMuscMSDia2\", "
					+ " s3.debilidad_muscular_ms \"debilidadMuscMSDia3\", "
					+ " s4.debilidad_muscular_ms \"debilidadMuscMSDia4\", "
					+ " s5.debilidad_muscular_ms \"debilidadMuscMSDia5\", "
					+ " s6.debilidad_muscular_ms \"debilidadMuscMSDia6\", "
					+ " s7.debilidad_muscular_ms \"debilidadMuscMSDia7\", "
					+ " s8.debilidad_muscular_ms \"debilidadMuscMSDia8\", "
					+ " s9.debilidad_muscular_ms \"debilidadMuscMSDia9\", "
					+ " s10.debilidad_muscular_ms \"debilidadMuscMSDia10\", "
					+ " s11.debilidad_muscular_ms \"debilidadMuscMSDia11\", "
					+ " s12.debilidad_muscular_ms \"debilidadMuscMSDia12\", "
					+ " s13.debilidad_muscular_ms \"debilidadMuscMSDia13\", "
					+ " s14.debilidad_muscular_ms \"debilidadMuscMSDia14\", "
					
					+ " s1.debilidad_muscular_mi \"debilidadMuscMIDia1\", "
					+ " s2.debilidad_muscular_mi \"debilidadMuscMIDia2\", "
					+ " s3.debilidad_muscular_mi \"debilidadMuscMIDia3\", "
					+ " s4.debilidad_muscular_mi \"debilidadMuscMIDia4\", "
					+ " s5.debilidad_muscular_mi \"debilidadMuscMIDia5\", "
					+ " s6.debilidad_muscular_mi \"debilidadMuscMIDia6\", "
					+ " s7.debilidad_muscular_mi \"debilidadMuscMIDia7\", "
					+ " s8.debilidad_muscular_mi \"debilidadMuscMIDia8\", "
					+ " s9.debilidad_muscular_mi \"debilidadMuscMIDia9\", "
					+ " s10.debilidad_muscular_mi \"debilidadMuscMIDia10\", "
					+ " s11.debilidad_muscular_mi \"debilidadMuscMIDia11\", "
					+ " s12.debilidad_muscular_mi \"debilidadMuscMIDia12\", "
					+ " s13.debilidad_muscular_mi \"debilidadMuscMIDia13\", "
					+ " s14.debilidad_muscular_mi \"debilidadMuscMIDia14\", "
					
					+ " s1.parestesia_ms \"parestesiaMSDia1\", "
					+ " s2.parestesia_ms \"parestesiaMSDia2\", "
					+ " s3.parestesia_ms \"parestesiaMSDia3\", "
					+ " s4.parestesia_ms \"parestesiaMSDia4\", "
					+ " s5.parestesia_ms \"parestesiaMSDia5\", "
					+ " s6.parestesia_ms \"parestesiaMSDia6\", "
					+ " s7.parestesia_ms \"parestesiaMSDia7\", "
					+ " s8.parestesia_ms \"parestesiaMSDia8\", "
					+ " s9.parestesia_ms \"parestesiaMSDia9\", "
					+ " s10.parestesia_ms \"parestesiaMSDia10\", "
					+ " s11.parestesia_ms \"parestesiaMSDia11\", "
					+ " s12.parestesia_ms \"parestesiaMSDia12\", "
					+ " s13.parestesia_ms \"parestesiaMSDia13\", "
					+ " s14.parestesia_ms \"parestesiaMSDia14\", "
					
					+ " s1.parestesia_mi \"parestesiaMIDia1\", "
					+ " s2.parestesia_mi \"parestesiaMIDia2\", "
					+ " s3.parestesia_mi \"parestesiaMIDia3\", "
					+ " s4.parestesia_mi \"parestesiaMIDia4\", "
					+ " s5.parestesia_mi \"parestesiaMIDia5\", "
					+ " s6.parestesia_mi \"parestesiaMIDia6\", "
					+ " s7.parestesia_mi \"parestesiaMIDia7\", "
					+ " s8.parestesia_mi \"parestesiaMIDia8\", "
					+ " s9.parestesia_mi \"parestesiaMIDia9\", "
					+ " s10.parestesia_mi \"parestesiaMIDia10\", "
					+ " s11.parestesia_mi \"parestesiaMIDia11\", "
					+ " s12.parestesia_mi \"parestesiaMIDia12\", "
					+ " s13.parestesia_mi \"parestesiaMIDia13\", "
					+ " s14.parestesia_mi \"parestesiaMIDia14\", "
					
					+ " s1.paralisis_muscular_ms \"paralisisMuscMSDia1\", "
					+ " s2.paralisis_muscular_ms \"paralisisMuscMSDia2\", "
					+ " s3.paralisis_muscular_ms \"paralisisMuscMSDia3\", "
					+ " s4.paralisis_muscular_ms \"paralisisMuscMSDia4\", "
					+ " s5.paralisis_muscular_ms \"paralisisMuscMSDia5\", "
					+ " s6.paralisis_muscular_ms \"paralisisMuscMSDia6\", "
					+ " s7.paralisis_muscular_ms \"paralisisMuscMSDia7\", "
					+ " s8.paralisis_muscular_ms \"paralisisMuscMSDia8\", "
					+ " s9.paralisis_muscular_ms \"paralisisMuscMSDia9\", "
					+ " s10.paralisis_muscular_ms \"paralisisMuscMSDia10\", "
					+ " s11.paralisis_muscular_ms \"paralisisMuscMSDia11\", "
					+ " s12.paralisis_muscular_ms \"paralisisMuscMSDia12\", "
					+ " s13.paralisis_muscular_ms \"paralisisMuscMSDia13\", "
					+ " s14.paralisis_muscular_ms \"paralisisMuscMSDia14\", "
					
					+ " s1.paralisis_muscular_mi \"paralisisMuscMIDia1\", "
					+ " s2.paralisis_muscular_mi \"paralisisMuscMIDia2\", "
					+ " s3.paralisis_muscular_mi \"paralisisMuscMIDia3\", "
					+ " s4.paralisis_muscular_mi \"paralisisMuscMIDia4\", "
					+ " s5.paralisis_muscular_mi \"paralisisMuscMIDia5\", "
					+ " s6.paralisis_muscular_mi \"paralisisMuscMIDia6\", "
					+ " s7.paralisis_muscular_mi \"paralisisMuscMIDia7\", "
					+ " s8.paralisis_muscular_mi \"paralisisMuscMIDia8\", "
					+ " s9.paralisis_muscular_mi \"paralisisMuscMIDia9\", "
					+ " s10.paralisis_muscular_mi \"paralisisMuscMIDia10\", "
					+ " s11.paralisis_muscular_mi \"paralisisMuscMIDia11\", "
					+ " s12.paralisis_muscular_mi \"paralisisMuscMIDia12\", "
					+ " s13.paralisis_muscular_mi \"paralisisMuscMIDia13\", "
					+ " s14.paralisis_muscular_mi \"paralisisMuscMIDia14\", "		
					
					+ " s1.tos \"tosDia1\", "
					+ " s2.tos \"tosDia2\", "
					+ " s3.tos \"tosDia3\", "
					+ " s4.tos \"tosDia4\", "
					+ " s5.tos \"tosDia5\", "
					+ " s6.tos \"tosDia6\", "
					+ " s7.tos \"tosDia7\", "
					+ " s8.tos \"tosDia8\", "
					+ " s9.tos \"tosDia9\", "
					+ " s10.tos \"tosDia10\", "
					+ " s11.tos \"tosDia11\", "
					+ " s12.tos \"tosDia12\", "
					+ " s13.tos \"tosDia13\", "
					+ " s14.tos \"tosDia14\", "
			
					+ " s1.rinorrea \"rinorreaDia1\", "
					+ " s2.rinorrea \"rinorreaDia2\", "
					+ " s3.rinorrea \"rinorreaDia3\", "
					+ " s4.rinorrea \"rinorreaDia4\", "
					+ " s5.rinorrea \"rinorreaDia5\", "
					+ " s6.rinorrea \"rinorreaDia6\", "
					+ " s7.rinorrea \"rinorreaDia7\", "
					+ " s8.rinorrea \"rinorreaDia8\", "
					+ " s9.rinorrea \"rinorreaDia9\", "
					+ " s10.rinorrea \"rinorreaDia10\", "
					+ " s11.rinorrea \"rinorreaDia11\", "
					+ " s12.rinorrea \"rinorreaDia12\", "
					+ " s13.rinorrea \"rinorreaDia13\", "
					+ " s14.rinorrea \"rinorreaDia14\", "
					
					+ " s1.dolor_garganta \"dolorGargantaDia1\", "
					+ " s2.dolor_garganta \"dolorGargantaDia2\", "
					+ " s3.dolor_garganta \"dolorGargantaDia3\", "
					+ " s4.dolor_garganta \"dolorGargantaDia4\", "
					+ " s5.dolor_garganta \"dolorGargantaDia5\", "
					+ " s6.dolor_garganta \"dolorGargantaDia6\", "
					+ " s7.dolor_garganta \"dolorGargantaDia7\", "
					+ " s8.dolor_garganta \"dolorGargantaDia8\", "
					+ " s9.dolor_garganta \"dolorGargantaDia9\", "
					+ " s10.dolor_garganta \"dolorGargantaDia10\", "
					+ " s11.dolor_garganta \"dolorGargantaDia11\", "
					+ " s12.dolor_garganta \"dolorGargantaDia12\", "
					+ " s13.dolor_garganta \"dolorGargantaDia13\", "
					+ " s14.dolor_garganta \"dolorGargantaDia14\", "
					
					+ " s1.prurito \"pruritoDia1\", "
					+ " s2.prurito \"pruritoDia2\", "
					+ " s3.prurito \"pruritoDia3\", "
					+ " s4.prurito \"pruritoDia4\", "
					+ " s5.prurito \"pruritoDia5\", "
					+ " s6.prurito \"pruritoDia6\", "
					+ " s7.prurito \"pruritoDia7\", "
					+ " s8.prurito \"pruritoDia8\", "
					+ " s9.prurito \"pruritoDia9\", "
					+ " s10.prurito \"pruritoDia10\", "
					+ " s11.prurito \"pruritoDia11\", "
					+ " s12.prurito \"pruritoDia12\", "
					+ " s13.prurito \"pruritoDia13\", "
					+ " s14.prurito \"pruritoDia14\", "
					
					
					+ " (select um.codigopersonal from usuarios_view um where s1.usuario_medico = um.id) \"nombreMedico1\", "
					+ " (select um.codigopersonal from usuarios_view um where s2.usuario_medico = um.id) \"nombreMedico2\", "
					+ " (select um.codigopersonal from usuarios_view um where s3.usuario_medico = um.id) \"nombreMedico3\", "
					+ " (select um.codigopersonal from usuarios_view um where s4.usuario_medico = um.id) \"nombreMedico4\", "
					+ " (select um.codigopersonal from usuarios_view um where s5.usuario_medico = um.id) \"nombreMedico5\", "
					+ " (select um.codigopersonal from usuarios_view um where s6.usuario_medico = um.id) \"nombreMedico6\", "
					+ " (select um.codigopersonal from usuarios_view um where s7.usuario_medico = um.id) \"nombreMedico7\", "
					+ " (select um.codigopersonal from usuarios_view um where s8.usuario_medico = um.id) \"nombreMedico8\", "
					+ " (select um.codigopersonal from usuarios_view um where s9.usuario_medico = um.id) \"nombreMedico9\", "
					+ " (select um.codigopersonal from usuarios_view um where s10.usuario_medico = um.id) \"nombreMedico10\", "
					+ " (select um.codigopersonal from usuarios_view um where s11.usuario_medico = um.id) \"nombreMedico11\", "
					+ " (select um.codigopersonal from usuarios_view um where s12.usuario_medico = um.id) \"nombreMedico12\", "
					+ " (select um.codigopersonal from usuarios_view um where s13.usuario_medico = um.id) \"nombreMedico13\", "
					+ " (select um.codigopersonal from usuarios_view um where s14.usuario_medico = um.id) \"nombreMedico14\", "
					
					+ " (select um.codigopersonal from usuarios_view um where s1.supervisor = um.id) \"supervisor1\", "
					+ " (select um.codigopersonal from usuarios_view um where s2.supervisor = um.id) \"supervisor2\", "
					+ " (select um.codigopersonal from usuarios_view um where s3.supervisor = um.id) \"supervisor3\", "
					+ " (select um.codigopersonal from usuarios_view um where s4.supervisor = um.id) \"supervisor4\", "
					+ " (select um.codigopersonal from usuarios_view um where s5.supervisor = um.id) \"supervisor5\", "
					+ " (select um.codigopersonal from usuarios_view um where s6.supervisor = um.id) \"supervisor6\", "
					+ " (select um.codigopersonal from usuarios_view um where s7.supervisor = um.id) \"supervisor7\", "
					+ " (select um.codigopersonal from usuarios_view um where s8.supervisor = um.id) \"supervisor8\", "
					+ " (select um.codigopersonal from usuarios_view um where s9.supervisor = um.id) \"supervisor9\", "
					+ " (select um.codigopersonal from usuarios_view um where s10.supervisor = um.id) \"supervisor10\", "
					+ " (select um.codigopersonal from usuarios_view um where s11.supervisor = um.id) \"supervisor11\", "
					+ " (select um.codigopersonal from usuarios_view um where s12.supervisor = um.id) \"supervisor12\", "
					+ " (select um.codigopersonal from usuarios_view um where s13.supervisor = um.id) \"supervisor13\", "
					+ " (select um.codigopersonal from usuarios_view um where s14.supervisor = um.id) \"supervisor14\", "
					
					
					+ " to_char(s1.fecha_seguimiento, 'dd/MM/yyyy') \"fechaSeguimiento1\", "
					+ " to_char(s2.fecha_seguimiento, 'dd/MM/yyyy') \"fechaSeguimiento2\", "
					+ " to_char(s3.fecha_seguimiento, 'dd/MM/yyyy') \"fechaSeguimiento3\", "
					+ " to_char(s4.fecha_seguimiento, 'dd/MM/yyyy') \"fechaSeguimiento4\", "
					+ " to_char(s5.fecha_seguimiento, 'dd/MM/yyyy') \"fechaSeguimiento5\", "
					+ " to_char(s6.fecha_seguimiento, 'dd/MM/yyyy') \"fechaSeguimiento6\", "
					+ " to_char(s7.fecha_seguimiento, 'dd/MM/yyyy') \"fechaSeguimiento7\", "
					+ " to_char(s8.fecha_seguimiento, 'dd/MM/yyyy') \"fechaSeguimiento8\", "
					+ " to_char(s9.fecha_seguimiento, 'dd/MM/yyyy') \"fechaSeguimiento9\", "
					+ " to_char(s10.fecha_seguimiento, 'dd/MM/yyyy') \"fechaSeguimiento10\", "
					+ " to_char(s11.fecha_seguimiento, 'dd/MM/yyyy') \"fechaSeguimiento11\", "
					+ " to_char(s12.fecha_seguimiento, 'dd/MM/yyyy') \"fechaSeguimiento12\", "
					+ " to_char(s12.fecha_seguimiento, 'dd/MM/yyyy') \"fechaSeguimiento13\", "
					+ " to_char(s12.fecha_seguimiento, 'dd/MM/yyyy') \"fechaSeguimiento14\" "
					
					+ " from paciente p  "
					+ " inner join hoja_zika h on p.cod_expediente = h.cod_expediente "
					+ " inner join seguimiento_zika s on h.sec_hoja_zika = s.sec_hoja_zika "
					+ " left join seguimiento_zika s1 on h.sec_hoja_zika = s1.sec_hoja_zika and s1.control_dia='1' "
					+ " left join seguimiento_zika s2 on h.sec_hoja_zika = s2.sec_hoja_zika and s2.control_dia='2' "
					+ " left join seguimiento_zika s3 on h.sec_hoja_zika = s3.sec_hoja_zika and s3.control_dia='3' "
					+ " left join seguimiento_zika s4 on h.sec_hoja_zika = s4.sec_hoja_zika and s4.control_dia='4' "
					+ " left join seguimiento_zika s5 on h.sec_hoja_zika = s5.sec_hoja_zika and s5.control_dia='5' "
					+ " left join seguimiento_zika s6 on h.sec_hoja_zika = s6.sec_hoja_zika and s6.control_dia='6' "
					+ " left join seguimiento_zika s7 on h.sec_hoja_zika = s7.sec_hoja_zika and s7.control_dia='7' "
					+ " left join seguimiento_zika s8 on h.sec_hoja_zika = s8.sec_hoja_zika and s8.control_dia='8' "
					+ " left join seguimiento_zika s9 on h.sec_hoja_zika = s9.sec_hoja_zika and s9.control_dia='9' "
					+ " left join seguimiento_zika s10 on h.sec_hoja_zika = s10.sec_hoja_zika and s10.control_dia='10' "
					+ " left join seguimiento_zika s11 on h.sec_hoja_zika = s11.sec_hoja_zika and s11.control_dia='11' "
					+ " left join seguimiento_zika s12 on h.sec_hoja_zika = s12.sec_hoja_zika and s12.control_dia='12' "
					+ " left join seguimiento_zika s13 on h.sec_hoja_zika = s13.sec_hoja_zika and s13.control_dia='13' "
					+ " left join seguimiento_zika s14 on h.sec_hoja_zika = s14.sec_hoja_zika and s14.control_dia='14' ";

			sql += " where  h.num_hoja_seguimiento = :numHojaSeguimiento ";
			// System.out.println(sql);
			Query query = HIBERNATE_RESOURCE
					.getSession()
					.createSQLQuery(sql)
					.setResultTransformer(
							Transformers
									.aliasToBean(SeguimientoZikaReporte.class))
					.setParameter("numHojaSeguimiento", numHojaSeguimiento);

			List result = query.list();

			return UtilitarioReporte.mostrarReporte(nombreReporte, null,
					result, false, null);

		} catch (Exception e) {
			e.printStackTrace();

		} finally {
			if (HIBERNATE_RESOURCE.getSession().isOpen()) {
				HIBERNATE_RESOURCE.close();
			}
		}
		return null;
	}
	
	/***
	 * Metodo que realiza la impresion de Seguimiento Zika.
	 * @param numHojaSeguimiento, Numero de seguimiento zika.
	 */
	public void imprimirSeguimientoZikaPdf(int numHojaSeguimiento) {

		UtilitarioReporte ureporte = new UtilitarioReporte();
		ureporte.imprimirDocumento("rptSeguimientoZika_"
				+ numHojaSeguimiento,
				getSeguimientoZikaPdf(numHojaSeguimiento));

	}
	
	
	
}
