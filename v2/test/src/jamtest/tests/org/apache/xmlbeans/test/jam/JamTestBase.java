/*
* The Apache Software License, Version 1.1
*
*
* Copyright (c) 2003 The Apache Software Foundation.  All rights
* reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions
* are met:
*
* 1. Redistributions of source code must retain the above copyright
*    notice, this list of conditions and the following disclaimer.
*
* 2. Redistributions in binary form must reproduce the above copyright
*    notice, this list of conditions and the following disclaimer in
*    the documentation and/or other materials provided with the
*    distribution.
*
* 3. The end-user documentation included with the redistribution,
*    if any, must include the following acknowledgment:
*       "This product includes software developed by the
*        Apache Software Foundation (http://www.apache.org/)."
*    Alternately, this acknowledgment may appear in the software itself,
*    if and wherever such third-party acknowledgments normally appear.
*
* 4. The names "Apache" and "Apache Software Foundation" must
*    not be used to endorse or promote products derived from this
*    software without prior written permission. For written
*    permission, please contact apache@apache.org.
*
* 5. Products derived from this software may not be called "Apache
*    XMLBeans", nor may "Apache" appear in their name, without prior
*    written permission of the Apache Software Foundation.
*
* THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
* WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
* OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
* ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
* SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
* LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
* USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
* OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
* OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
* SUCH DAMAGE.
* ====================================================================
*
* This software consists of voluntary contributions made by many
* individuals on behalf of the Apache Software Foundation and was
* originally based on software copyright (c) 2003 BEA Systems
* Inc., <http://www.bea.com/>. For more information on the Apache Software
* Foundation, please see <http://www.apache.org/>.
*/
package org.apache.xmlbeans.test.jam;

import junit.framework.TestCase;
import org.apache.xmlbeans.impl.jam.*;

import java.io.File;
import java.io.IOException;

/**
 * <p>Abstract base class for basic jam test cases.  These test cases work
 * against an abstract JService - they don't care how the java types
 * were loaded.  Extending classes are responsible for implementing the
 * getService() method which should create the service from sources, or
 * classes, or whatever is appropriate.</p>
 *
 * @author Patrick Calahan <pcal@bea.com>
 */
public abstract class JamTestBase extends TestCase {

  // ========================================================================
  // Constants

  protected static final String
          DUMMY = "org.apache.xmlbeans.test.jam.dummyclasses";

  // ========================================================================
  // Variables

  private JService mService = null;
  private JClassLoader mLoader = null;

  // ========================================================================
  // Constructors
  
  public JamTestBase() {
    super("JamTestBase");
  }

  public JamTestBase(String casename) {
    super(casename);
  }

  // ========================================================================
  // Abstract methods

  /**
   * Called during setup() to return the parameters which we can use to
   * create a service that contains all of java types under dummyclasses.
   */
  protected abstract JServiceParams getBasicServiceParams() throws Exception;

  //kind of a quick hack for now, should remove this and make sure that
  //even the classes case make the annotations available using a special
  //JStore
  protected abstract boolean isAnnotationsAvailable();

  // ========================================================================
  // Utility methods

  /**
   * Returns the directory in which the sources for the dummyclasses live.
   */
  protected File getDummyclassesSourceRoot() {
    return new File("dummyclasses");
  }

  /**
   * Returns the directory into which the dummyclasses have been compiled.
   */
  protected File getDummyclassesClassDir() {
    return new File("../../../build/test/jamtest/dummyclasses");
  }

  // ========================================================================
  // TestCase implementation

  public void setUp() throws Exception {
    JServiceFactory jsf = JServiceFactory.getInstance();
    mService = jsf.createService(getBasicServiceParams());
    if (mService == null) throw new IllegalArgumentException("null service");
    mLoader = mService.getClassLoader();
    if (mLoader == null) throw new IllegalArgumentException("null loader");
  }

  // ========================================================================
  // Test methods

  public void testInterfaceIsAssignableFrom()
    throws ClassNotFoundException 
  {
    JClass fooImpl = mLoader.loadClass(DUMMY+".FooImpl");
    JClass foo = mLoader.loadClass(DUMMY+".Foo");
    assertTrue("Foo should be assignableFrom FooImpl",
               foo.isAssignableFrom(fooImpl));
    assertTrue("FooImpl should not be assignableFrom Foo",
               !fooImpl.isAssignableFrom(foo));
  }

  public void testClassIsAssignableFrom() 
    throws ClassNotFoundException 
  {
    JClass fooImpl = mLoader.loadClass(DUMMY+".FooImpl");
    JClass base = mLoader.loadClass(DUMMY+".Base");
    assertTrue("Base should be assignableFrom FooImpl",
               base.isAssignableFrom(fooImpl));
    assertTrue("FooImpl should not be assignableFrom Base",
               !fooImpl.isAssignableFrom(base));
  }

  public void testClassIsAssignableFromDifferentClassLoaders() 
    throws ClassNotFoundException 
  {
    JClass baz = mLoader.loadClass(DUMMY+".Baz");
    JClass runnable = mLoader.loadClass("java.lang.Runnable");
    assertTrue("Runnable should be assignableFrom Baz",
               runnable.isAssignableFrom(baz));
    assertTrue("Baz should not be assignableFrom Runnable",
               !baz.isAssignableFrom(runnable));
  }


  public void testAnnotationsAndInheritance() {
    JClass ejb = mLoader.loadClass(DUMMY+".ejb.TraderEJB");
    JClass ienv = ejb.getInterfaces()[0];
    JMethod ejbBuy = ejb.getMethods()[0];
    JMethod ienvBuy = ienv.getMethods()[0];
    String INTER_ANN = "ejbgen:remote-method@transaction-attribute";
    String INTER_ANN_VALUE = "NotSupported";
    String CLASS_ANN = "ejbgen:remote-method@isolation-level";
    String CLASS_ANN_VALUE = "Serializable";

    verifyAnnotationAbsent(ejbBuy,INTER_ANN);
    verifyAnnotationAbsent(ienvBuy,CLASS_ANN);

    if (isAnnotationsAvailable()) {
      verifyAnnotation(ienvBuy,INTER_ANN,INTER_ANN_VALUE);
      verifyAnnotation(ejbBuy,CLASS_ANN,CLASS_ANN_VALUE);
    } else {
      verifyAnnotationAbsent(ienvBuy,INTER_ANN);
      verifyAnnotationAbsent(ejbBuy,CLASS_ANN);
    }
  }



  // ========================================================================
  // Private methods

  private void verifyAnnotation(JElement j, String ann, String val) {
    JAnnotation a = j.getAnnotation(ann);
    assertTrue(j.getParent().getQualifiedName()+" '"+j.getQualifiedName()+"' is missing expected annotation '"+ann+"'",
                a != null);
    assertTrue(j.getQualifiedName()+"  annotation '"+ann+"' does not equal "+
               val,val.equals(a.getStringValue().trim()));
  }

  private void verifyAnnotationAbsent(JElement j, String ann) {
    JAnnotation a = j.getAnnotation(ann);
    assertTrue("'"+j.getQualifiedName()+"' expected to NOT have annotation '"+ann+"'",
                a == null);
  }



}
