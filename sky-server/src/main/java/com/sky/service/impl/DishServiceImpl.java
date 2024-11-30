package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.annotation.AutoFill;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.enumeration.OperationType;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.CacheService;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String DISH_KEY_PREFIX = "dish:";

    @Override
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.insert(dish);
        Long dishId = dish.getId();

        // 保存口味数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && !flavors.isEmpty()) {
            flavors.forEach(flavor -> flavor.setDishId(dishId));
            dishFlavorMapper.insertBatch(flavors);
        }

        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(flavors);
        String key = DISH_KEY_PREFIX + dishId;
        cacheService.setWithBloomFilter(key, dishVO);
    }

    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        List<DishVO> dishList = page.getResult();

        // 对每个菜品进行缓存处理
        if (dishList != null && !dishList.isEmpty()) {
            dishList.forEach(dishVO -> {
                String key = DISH_KEY_PREFIX + dishVO.getId();
                // 检查缓存中是否存在,不存在才添加
                if (cacheService.getWithBloomFilter(key, Dish.class) == null) {
                    cacheService.setWithBloomFilter(key, dishVO);
                }
            });
        }
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        for (Long id : ids) {
            Dish dish = dishMapper.getById(id);
            if (dish.getStatus() == StatusConstant.ENABLE) {
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        List<Long> setmealIds = setmealDishMapper.getSetmealDishIdsByDishIds(ids);
        if (setmealIds != null && setmealIds.size() > 0) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        for (Long id : ids) {
            dishMapper.deleteById(id);
            dishFlavorMapper.deleteByDishId(id);
            String key = DISH_KEY_PREFIX + id;
            redisTemplate.delete(key);
        }
    }

    @Override
    public DishVO getByIdWithFlavor(Long id) {
        String key = DISH_KEY_PREFIX + id;

        // 从缓存获取
        DishVO dishVO = (DishVO) cacheService.getWithBloomFilter(key, DishVO.class);
        if (dishVO != null) {
            return dishVO;
        }

        // 缓存未命中，查询完整信息
        Dish dish = dishMapper.getById(id);
        if (dish != null) {
            dishVO = new DishVO();
            BeanUtils.copyProperties(dish, dishVO);
            // 查询口味信息
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(id);
            dishVO.setFlavors(flavors);
            // 缓存VO对象
            cacheService.setWithBloomFilter(key, dishVO);
        }
        return dishVO;
    }

    @Override
    @Transactional
    public void updateWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.update(dish);
        dishFlavorMapper.deleteByDishId(dishDTO.getId());
        List<DishFlavor> dishFlavors = dishDTO.getFlavors();
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        if (dishFlavors != null && dishFlavors.size() > 0) {
            for (DishFlavor f : dishFlavors) {
                f.setDishId(dish.getId());
            }
            dishFlavorMapper.insertBatch(dishFlavors);
            dishVO.setFlavors(dishFlavors);
        }
        String key = DISH_KEY_PREFIX + dishDTO.getId();
        cacheService.setWithBloomFilter(key, dishVO);
    }

    @Override
    public void updateStatus(Integer status, Long id) {
        Dish dish = new Dish();
        dish.setStatus(status);
        dish.setId(id);
        dishMapper.update(dish);
        String key = DISH_KEY_PREFIX + id;
        DishVO dishVO = (DishVO) cacheService.getWithBloomFilter(key, DishVO.class);
        if (dishVO != null) {
            dishVO.setStatus(status);
            cacheService.setWithBloomFilter(key, dishVO);
        } else {
            dishVO = new DishVO();
            BeanUtils.copyProperties(dish, dishVO);
            List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);
            dishVO.setFlavors(dishFlavors);
            cacheService.setWithBloomFilter(key, dishVO);
        }
    }

    @Override
    public List<Dish> getByCategoryId(Long categoryId) {
        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);
        return dishMapper.list(dish);
    }

}
