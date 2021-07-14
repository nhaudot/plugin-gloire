-- phpMyAdmin SQL Dump
-- version 4.9.7
-- https://www.phpmyadmin.net/
--
-- Hôte : localhost:3306
-- Généré le : mer. 14 juil. 2021 à 18:12
-- Version du serveur :  5.7.32
-- Version de PHP : 7.4.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";

--
-- Base de données : `gloire`
--

-- --------------------------------------------------------

--
-- Structure de la table `statistiques`
--

CREATE TABLE `statistiques` (
                                `uuid` text NOT NULL,
                                `joueur` text NOT NULL,
                                `gloire` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Déchargement des données de la table `statistiques`
--

INSERT INTO `statistiques` (`uuid`, `joueur`, `gloire`) VALUES
('fefc751a-2ffc-32f3-85b1-0e5048615e11', 'nathan060700', 106),
('9d7a169f-e7af-39c2-abd0-6e4bca3c789f', '_Kelua', 109);
