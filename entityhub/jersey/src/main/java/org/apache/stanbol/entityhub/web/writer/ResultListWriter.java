package org.apache.stanbol.entityhub.web.writer;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.web.ModelWriter;
import org.apache.stanbol.entityhub.web.ModelWriterRegistry;
import org.codehaus.jettison.json.JSONException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate=true)
@Service(Object.class)
@Property(name="javax.ws.rs", boolValue=true)
@Provider
public class ResultListWriter implements MessageBodyWriter<QueryResultList<?>> {

    private final Logger log = LoggerFactory.getLogger(ResultListWriter.class);
        
    @Reference
    protected ModelWriterRegistry writerRegistry;
    
    @Override
    public long getSize(QueryResultList<?> l,
                        Class<?> type,
                        Type genericType,
                        Annotation[] annotations,
                        MediaType mediaType) {
        return -1;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        if(QueryResultList.class.isAssignableFrom(type)){
            if(mediaType.isWildcardType() && mediaType.isWildcardSubtype()){
                mediaType = ModelWriter.DEFAULT_MEDIA_TYPE;
            }
            return writerRegistry.isWriteable(getMatchType(mediaType), null);
        } else {
            return false;
        }
    }

    @Override
    public void writeTo(QueryResultList<?> list,
                        Class<?> type,
                        Type genericType,
                        Annotation[] annotations,
                        MediaType mediaType,
                        MultivaluedMap<String,Object> httpHeaders,
                        OutputStream entityStream) throws IOException, WebApplicationException {
        //check for wildcard
        if(mediaType.isWildcardType() && mediaType.isWildcardSubtype()){
            mediaType = ModelWriter.DEFAULT_MEDIA_TYPE;
        }
        String charset = mediaType.getParameters().get("charset");
        if(charset == null){
            charset = ModelWriter.DEFAULT_CHARSET;
            mediaType = mediaType.withCharset(charset);
            httpHeaders.putSingle(HttpHeaders.CONTENT_TYPE, mediaType.toString());
        }
        Class<? extends Representation> nativeType;
        if(list.isEmpty()){ //for empty lists
            nativeType = null; //the type does not matter
        } else if(Representation.class.isAssignableFrom(list.getType())){
            nativeType = ((Representation)list.iterator().next()).getClass();
        } else if(Entity.class.isAssignableFrom(list.getType())){
            nativeType = ((Entity)list.iterator().next()).getRepresentation().getClass();
        } else { //only a list of string ids
            nativeType = null;
        }
        Iterator<ServiceReference> refs = writerRegistry.getModelWriters(
            getMatchType(mediaType), nativeType).iterator();
        ModelWriter writer = null;
        MediaType selectedMediaType = null;
        while((writer == null || selectedMediaType == null) && refs.hasNext()){
            writer = writerRegistry.getService(refs.next());
            if(writer != null){
                if(mediaType.isWildcardType() || mediaType.isWildcardSubtype()){
                    selectedMediaType = writer.getBestMediaType(mediaType);
                } else {
                    selectedMediaType = mediaType;
                }
            }
        }
        selectedMediaType = selectedMediaType.withCharset(charset);
        httpHeaders.putSingle(HttpHeaders.CONTENT_TYPE, mediaType.toString());
        if(writer == null || selectedMediaType == null){
            throw new WebApplicationException("Unable to serialize ResultList with "
                    + list.getType()+" (nativeType: "+nativeType+") to "+mediaType);
        }
        log.debug("serialize ResultList of {} with ModelWriter {}",nativeType, writer.getClass().getName());
        writer.write(list, entityStream, selectedMediaType);
    }

    /**
     * Strips all parameters from the parsed mediaType
     * @param mediaType
     */
    protected MediaType getMatchType(MediaType mediaType) {
        final MediaType matchType;
        if(!mediaType.getParameters().isEmpty()){
            matchType = new MediaType(mediaType.getType(), mediaType.getSubtype());
        } else {
            matchType = mediaType;
        }
        return matchType;
    }
}