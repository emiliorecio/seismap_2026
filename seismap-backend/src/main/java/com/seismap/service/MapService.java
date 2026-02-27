package com.seismap.service;

import com.seismap.model.entity.SeismapMap;
import com.seismap.repository.SeismapMapRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MapService {

    private final SeismapMapRepository mapRepository;

    public MapService(SeismapMapRepository mapRepository) {
        this.mapRepository = mapRepository;
    }

    public SeismapMap getById(Long id) {
        return mapRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Map not found: " + id));
    }

    public java.util.Optional<SeismapMap> getDefault(Long userId) {
        return mapRepository.findFirstByUserIdOrderByIdAsc(userId);
    }

    public List<SeismapMap> listByUser(Long userId) {
        return mapRepository.findByUserId(userId);
    }

    @Transactional
    public SeismapMap create(SeismapMap map) {
        return mapRepository.save(map);
    }

    @Transactional
    public SeismapMap rename(Long id, String newName) {
        SeismapMap map = getById(id);
        map.setName(newName);
        return mapRepository.save(map);
    }

    @Transactional
    public void delete(Long id) {
        mapRepository.deleteById(id);
    }

    @Transactional
    public SeismapMap update(Long id, SeismapMap updated) {
        SeismapMap map = getById(id);
        map.setName(updated.getName());
        map.setDescription(updated.getDescription());
        map.setCenter(updated.getCenter());
        map.setZoom(updated.getZoom());
        map.setMinDateType(updated.getMinDateType());
        map.setMinDateRelativeAmount(updated.getMinDateRelativeAmount());
        map.setMinDateRelativeUnits(updated.getMinDateRelativeUnits());
        map.setMinDate(updated.getMinDate());
        map.setMaxDateType(updated.getMaxDateType());
        map.setMaxDateRelativeAmount(updated.getMaxDateRelativeAmount());
        map.setMaxDateRelativeUnits(updated.getMaxDateRelativeUnits());
        map.setMaxDate(updated.getMaxDate());
        map.setMinDepthType(updated.getMinDepthType());
        map.setMinDepth(updated.getMinDepth());
        map.setMaxDepthType(updated.getMaxDepthType());
        map.setMaxDepth(updated.getMaxDepth());
        map.setMagnitudeType(updated.getMagnitudeType());
        map.setMinMagnitudeType(updated.getMinMagnitudeType());
        map.setMinMagnitude(updated.getMinMagnitude());
        map.setMaxMagnitudeType(updated.getMaxMagnitudeType());
        map.setMaxMagnitude(updated.getMaxMagnitude());
        map.setListUnmeasured(updated.isListUnmeasured());
        map.setAnimationType(updated.getAnimationType());
        map.setAnimationStepKeep(updated.getAnimationStepKeep());
        map.setAnimationSteps(updated.getAnimationSteps());
        map.setAnimationStepDuration(updated.getAnimationStepDuration());
        map.setReverseAnimation(updated.isReverseAnimation());
        map.setStyle(updated.getStyle());
        return mapRepository.save(map);
    }
}
