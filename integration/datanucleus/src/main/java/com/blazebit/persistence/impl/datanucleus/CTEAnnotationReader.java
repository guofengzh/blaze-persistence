/*
 * Copyright 2014 - 2016 Blazebit.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.blazebit.persistence.impl.datanucleus;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.api.jpa.metadata.JPAAnnotationReader;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.IdentityType;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.metadata.PackageMetaData;

import com.blazebit.persistence.CTE;

public class CTEAnnotationReader extends JPAAnnotationReader {
    
    public CTEAnnotationReader(MetaDataManager mgr) {
        super(mgr);
        String[] supportedAnnotationPacakges = new String[supportedPackages.length + 1];
        System.arraycopy(supportedPackages, 0, supportedAnnotationPacakges, 0, supportedPackages.length);
        supportedAnnotationPacakges[supportedAnnotationPacakges.length - 1] = "com.blazebit.persistence";
        setSupportedAnnotationPackages(supportedAnnotationPacakges);
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public AbstractClassMetaData getMetaDataForClass(Class cls, PackageMetaData pmd, ClassLoaderResolver clr) {
        AbstractClassMetaData cmd = super.getMetaDataForClass(cls, pmd, clr);
        
        if (cmd == null) {
            return null;
        }
        
        if (!cls.isAnnotationPresent(CTE.class)) {
            return cmd;
        }

        cmd.setIdentityType(IdentityType.NONDURABLE);
        cmd.addExtension("view-definition", "--");
        // TODO: check that no collections are mapped
        
        for (int i = 0; i < cmd.getNoOfMembers(); i++) {
            AbstractMemberMetaData mmd = cmd.getMetaDataForMemberAtRelativePosition(i);
            if (mmd.isPrimaryKey()) {
                mmd.setPrimaryKey(false);
            }
        }
        return cmd;
    }

}
