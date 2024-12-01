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
import com.sky.service.RedisSentinelService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

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
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedisSentinelService redisSentinelService;

    private static final String DISH_KEY_PREFIX = "dish:";

    /**
     * 新增菜品和对应的口味
     */
    @Override
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
        try {
            // 1. 保存到数据库
            Dish dish = new Dish();
            BeanUtils.copyProperties(dishDTO, dish);
            dishMapper.insert(dish);
            Long dishId = dish.getId();

            List<DishFlavor> flavors = dishDTO.getFlavors();
            if (flavors != null && !flavors.isEmpty()) {
                flavors.forEach(flavor -> flavor.setDishId(dishId));
                dishFlavorMapper.insertBatch(flavors);
            }

            // 2. 构建缓存数据
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(dish, dishVO);
            dishVO.setFlavors(flavors);

            // 3. 写入Redis主节点
            String key = DISH_KEY_PREFIX + dishId;
            String currentMaster = redisSentinelService.getCurrentMaster();
            log.info("当前Redis主节点: {}, 写入key: {}", currentMaster, key);
            redisTemplate.opsForValue().set(key, dishVO);

        } catch (Exception e) {
            log.error("保存菜品失败: {}", e.getMessage());
            throw new RuntimeException("保存菜品失败", e);
        }
    }

    /**
     * 分页查询菜品
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        try {
            PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
            Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);

            // 对每个菜品进行缓存处理
            if (page.getResult() != null && !page.getResult().isEmpty()) {
                for (DishVO dishVO : page.getResult()) {
                    String key = DISH_KEY_PREFIX + dishVO.getId();
                    // 检查从节点中是否存在数据
                    if (redisTemplate.opsForValue().get(key) == null) {
                        // 不存在则写入主节点
                        String currentMaster = redisSentinelService.getCurrentMaster();
                        log.info("当前Redis主节点: {}, 写入key: {}", currentMaster, key);
                        redisTemplate.opsForValue().set(key, dishVO);
                    }
                }
            }

            return new PageResult(page.getTotal(), page.getResult());
        } catch (Exception e) {
            log.error("分页查询失败: {}", e.getMessage());
            throw new RuntimeException("分页查询失败", e);
        }
    }

    /**
     * 批量删除菜品
     */
    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        try {
            // 1. 检查菜品状态
            for (Long id : ids) {
                Dish dish = dishMapper.getById(id);
                if (dish.getStatus() == StatusConstant.ENABLE) {
                    throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
                }
            }

            // 2. 检查是否被套餐关联
            List<Long> setmealIds = setmealDishMapper.getSetmealDishIdsByDishIds(ids);
            if (setmealIds != null && !setmealIds.isEmpty()) {
                throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
            }

            // 3. 删除数据库数据
            for (Long id : ids) {
                dishMapper.deleteById(id);
                dishFlavorMapper.deleteByDishId(id);

                // 4. 删除Redis缓存
                String key = DISH_KEY_PREFIX + id;
                String currentMaster = redisSentinelService.getCurrentMaster();
                log.info("当前Redis主节点: {}, 删除key: {}", currentMaster, key);
                redisTemplate.delete(key);
            }
        } catch (Exception e) {
            log.error("删除菜品失败: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 根据id查询菜品和口味
     */
    @Override
    public DishVO getByIdWithFlavor(Long id) {
        String key = DISH_KEY_PREFIX + id;
        try {
            // 1. 先从Redis从节点读取
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.info("从Redis从节点读取数据成功, key: {}", key);
                // 使用ObjectMapper进行类型转换
                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());
                return mapper.convertValue(cached, DishVO.class);
            }

            // 2. 缓存未命中，从数据库查询
            Dish dish = dishMapper.getById(id);
            if (dish != null) {
                DishVO dishVO = new DishVO();
                BeanUtils.copyProperties(dish, dishVO);
                List<DishFlavor> flavors = dishFlavorMapper.getByDishId(id);
                dishVO.setFlavors(flavors);

                // 3. 写入Redis主节点
                String currentMaster = redisSentinelService.getCurrentMaster();
                log.info("当前Redis主节点: {}, 写入key: {}", currentMaster, key);
                redisTemplate.opsForValue().set(key, dishVO);

                return dishVO;
            }
            return null;
        } catch (Exception e) {
            log.error("查询菜品失败: {}", e.getMessage());
            throw new RuntimeException("查询菜品失败", e);
        }
    }

    /**
     * 修改菜品和对应的口味
     */
    @Override
    @Transactional
    public void updateWithFlavor(DishDTO dishDTO) {
        try {
            // 1. 更新数据库
            Dish dish = new Dish();
            BeanUtils.copyProperties(dishDTO, dish);
            dishMapper.update(dish);

            // 2. 删除原有口味
            dishFlavorMapper.deleteByDishId(dishDTO.getId());

            // 3. 添加新的口味
            List<DishFlavor> flavors = dishDTO.getFlavors();
            if (flavors != null && !flavors.isEmpty()) {
                flavors.forEach(flavor -> flavor.setDishId(dish.getId()));
                dishFlavorMapper.insertBatch(flavors);
            }

            // 4. 更新Redis缓存
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(dish, dishVO);
            dishVO.setFlavors(flavors);

            String key = DISH_KEY_PREFIX + dishDTO.getId();
            String currentMaster = redisSentinelService.getCurrentMaster();
            log.info("当前Redis主节点: {}, 更新key: {}", currentMaster, key);
            redisTemplate.opsForValue().set(key, dishVO);

        } catch (Exception e) {
            log.error("修改菜品失败: {}", e.getMessage());
            throw new RuntimeException("修改菜品失败", e);
        }
    }

    /**
     * 修改菜品状态
     */
    @Override
    public void updateStatus(Integer status, Long id) {
        try {
            // 1. 更新数据库
            Dish dish = new Dish();
            dish.setStatus(status);
            dish.setId(id);
            dishMapper.update(dish);

            // 2. 更新Redis缓存
            String key = DISH_KEY_PREFIX + id;
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                // 使用ObjectMapper进行类型转换
                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());
                DishVO dishVO = mapper.convertValue(cached, DishVO.class);
                dishVO.setStatus(status);

                String currentMaster = redisSentinelService.getCurrentMaster();
                log.info("当前Redis主节点: {}, 更新key: {}", currentMaster, key);
                redisTemplate.opsForValue().set(key, dishVO);
            }
        } catch (Exception e) {
            log.error("修改菜品状态失败: {}", e.getMessage());
            throw new RuntimeException("修改菜品状态失败", e);
        }
    }

    /**
     * 根据分类id查询菜品
     */
    @Override
    public List<Dish> getByCategoryId(Long categoryId) {
        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);
        return dishMapper.list(dish);
    }
}
