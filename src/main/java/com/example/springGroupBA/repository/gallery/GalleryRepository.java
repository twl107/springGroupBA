package com.example.springGroupBA.repository.gallery;

import com.example.springGroupBA.entity.gallery.Gallery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GalleryRepository extends JpaRepository<Gallery,Long> {
}
