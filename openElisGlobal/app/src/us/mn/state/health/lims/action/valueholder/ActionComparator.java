/**
* The contents of this file are subject to the Mozilla Public License
* Version 1.1 (the "License"); you may not use this file except in
* compliance with the License. You may obtain a copy of the License at
* http://www.mozilla.org/MPL/ 
* 
* Software distributed under the License is distributed on an "AS IS"
* basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
* License for the specific language governing rights and limitations under
* the License.
* 
* The Original Code is OpenELIS code.
* 
* Copyright (C) The Minnesota Department of Health.  All Rights Reserved.
*/
package us.mn.state.health.lims.action.valueholder;

import java.util.Comparator;

public class ActionComparator implements Comparable {
   String name;

   
   // You can put the default sorting capability here
   public int compareTo(Object obj) {
      Action a = (Action)obj;
      return this.name.compareTo(a.getActionDisplayValue());
   }
   
 

 
   public static final Comparator NAME_COMPARATOR =
     new Comparator() {
      public int compare(Object a, Object b) {
    	  Action a_a = (Action)a;
    	  Action a_b = (Action)b;
 
         return ((a_a.getActionDisplayValue().toLowerCase()).compareTo(a_b.getActionDisplayValue().toLowerCase()));

      }
   };
   

}
