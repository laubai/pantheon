package com.redhat.pantheon.model.document;

import com.redhat.pantheon.model.api.Child;
import com.redhat.pantheon.model.api.Field;
import com.redhat.pantheon.model.api.SlingModels;
import com.redhat.pantheon.model.workspace.WorkspaceChild;
import org.apache.sling.api.resource.PersistenceException;

import javax.inject.Named;
import javax.jcr.RepositoryException;

import java.util.Calendar;

import static com.google.common.collect.Streams.stream;
import static com.redhat.pantheon.jcr.JcrResources.rename;

/**
 * A specific Document variant node which houses all the versions for a specific language in the Document.
 */
public interface DocumentVariant extends WorkspaceChild {

    String DEFAULT_VARIANT_NAME = "DEFAULT";

    Child<? extends DocumentVersion> draft();

    Child<? extends DocumentVersion> released();

    default boolean hasDraft() {
        return draft().get() != null;
    }

    default Child<? extends  DocumentVersion> latestVersion() {
        return hasDraft() ? draft() : released();
    }

    @Named("jcr:uuid")
    Field<String> uuid();

    @Override
    DocumentVariants getParent();

    default void releaseDraft() {
        if(draft().get() == null) {
            throw new RuntimeException("There is no draft to release");
        }

        try {
            // ensure the released version is discarded
            if( released().get() != null ) {
                getResourceResolver().delete(released().get());
            }
            rename(draft().get(), "released");
            DocumentMetadata metadata = released().get()
                    .metadata().get();
            Calendar now = Calendar.getInstance();
            metadata.datePublished().set(now);
            if (metadata.dateFirstPublished().get() == null) {
                metadata.dateFirstPublished().set(now);
            }
        } catch (PersistenceException | RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    default void revertReleased() {
        if(released().get() == null) {
            throw new RuntimeException("There is no released version to revert");
        }

        try {
            // if there is no draft version, set the recently unpublished one as draft
            // it is guaranteed to be the latest one
            if(draft().get() == null) {
                rename(released().get(), "draft");
            } else {
                // released + draft
                released().get().delete();
            }

        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        } catch (PersistenceException e) {
            throw new RuntimeException(e);
        }
    }

    default DocumentLocale getParentLocale() {
        return SlingModels.getModel(this.getParent().getParent(), DocumentLocale.class);
    }

}
