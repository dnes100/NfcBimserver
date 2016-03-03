/**
 * Copyright (C) 2009-2014 BIMserver.org
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.bimserver.models.ifc4.impl;

import org.bimserver.models.ifc4.Ifc4Package;
import org.bimserver.models.ifc4.IfcRationalBSplineCurveWithKnots;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Ifc Rational BSpline Curve With Knots</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link org.bimserver.models.ifc4.impl.IfcRationalBSplineCurveWithKnotsImpl#getWeightsData <em>Weights Data</em>}</li>
 *   <li>{@link org.bimserver.models.ifc4.impl.IfcRationalBSplineCurveWithKnotsImpl#getWeightsDataAsString <em>Weights Data As String</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class IfcRationalBSplineCurveWithKnotsImpl extends IfcBSplineCurveWithKnotsImpl implements IfcRationalBSplineCurveWithKnots {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected IfcRationalBSplineCurveWithKnotsImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return Ifc4Package.Literals.IFC_RATIONAL_BSPLINE_CURVE_WITH_KNOTS;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@SuppressWarnings("unchecked")
	public EList<Double> getWeightsData() {
		return (EList<Double>) eGet(Ifc4Package.Literals.IFC_RATIONAL_BSPLINE_CURVE_WITH_KNOTS__WEIGHTS_DATA, true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@SuppressWarnings("unchecked")
	public EList<String> getWeightsDataAsString() {
		return (EList<String>) eGet(Ifc4Package.Literals.IFC_RATIONAL_BSPLINE_CURVE_WITH_KNOTS__WEIGHTS_DATA_AS_STRING, true);
	}

} //IfcRationalBSplineCurveWithKnotsImpl
